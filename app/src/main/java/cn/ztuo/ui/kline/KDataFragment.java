package cn.ztuo.ui.kline;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ViewPortHandler;
import cn.ztuo.R;
import cn.ztuo.ui.home.MainActivity;
import cn.ztuo.ui.mychart.CoupleChartGestureListener;
import cn.ztuo.ui.mychart.DataParse;
import cn.ztuo.ui.mychart.KLineBean;
import cn.ztuo.ui.mychart.KMAEntity;
import cn.ztuo.ui.mychart.MyBottomMarkerView;
import cn.ztuo.ui.mychart.MyCombinedChart;
import cn.ztuo.ui.mychart.MyHMarkerView;
import cn.ztuo.ui.mychart.MyLeftMarkerView;
import cn.ztuo.ui.mychart.MyUtils;
import cn.ztuo.ui.mychart.VMAEntity;
import cn.ztuo.ui.mychart.VolFormatter;
import cn.ztuo.base.BaseLazyFragment;
import cn.ztuo.entity.Currency;
import cn.ztuo.utils.WonderfulDateUtils;
import cn.ztuo.utils.WonderfulLogUtils;
import cn.ztuo.utils.WonderfulMathUtils;
import cn.ztuo.utils.WonderfulToastUtils;

import org.json.JSONArray;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import cn.ztuo.app.Injection;

import static cn.ztuo.ui.kline.KDataFragment.Type.MIN5;

//tradingview ??????
public class KDataFragment extends BaseLazyFragment implements KlineContract.View {
    String symbol = "BTC/USDT";
    String resolution;
    @BindView(R.id.tvKInfo)
    TextView tvKInfo;
    @BindView(R.id.tvMaInfo)
    TextView tvMaInfo;
    @BindView(R.id.kDataText)
    TextView mDataText;
    @BindView(R.id.kDataOne)
    TextView mDataOne;
    @BindView(R.id.kDataTwo)
    TextView mDataTwo;
    @BindView(R.id.kDataThree)
    TextView mDataThree;
    @BindView(R.id.kDataFour)
    TextView mDataFour;
    @BindView(R.id.kDataFive)
    TextView mDataFive;
    String color = "#66A7ADBC";
    String textColor = "#20232C";
    private boolean isHeader = false;
    private boolean isFooter = false;
    private boolean isFirst = true;
    private Type type = MIN5;
    @BindView(R.id.combinedK)
    MyCombinedChart mChartKline;//K??????
    @BindView(R.id.combinedChartDeal)
    MyCombinedChart mChartVolume;//?????????
    private Currency currency;
    //X???????????????
    protected XAxis xAxisKline, xAxisVolume, xAxisCharts;
    //Y???????????????
    protected YAxis axisLeftKline, axisLeftVolume, axisLeftCharts;
    //Y???????????????
    protected YAxis axisRightKline, axisRightVolume, axisRightCharts;
    //K????????????
    private ArrayList<KLineBean> kLineDatas;

    private ArrayList<Entry> baseLine = new ArrayList<>();

    private DataParse kData;
    private DataParse mCacheData;
    boolean isNeedStop = false;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                mChartKline.setAutoScaleMinMaxEnabled(true);
                mChartVolume.setAutoScaleMinMaxEnabled(true);

                mChartKline.notifyDataSetChanged();
                mChartVolume.notifyDataSetChanged();

                mChartKline.invalidate();
                mChartVolume.invalidate();

            } catch (Exception ex) {

            }
        }
    };
    RefreshThread refreshThread;
    private KlineContract.Presenter presenter;

    public static KDataFragment getInstance(Type type, String symbol) {
        KDataFragment kDataFragment = new KDataFragment();
        Bundle bundle = new Bundle();
        bundle.putString("symbol", symbol);
        bundle.putSerializable("type", type);
        kDataFragment.setArguments(bundle);
        return kDataFragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isNeedStop = true;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_kdata;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        type = (Type) getArguments().getSerializable("type");
        symbol = getArguments().getString("symbol");
        new KlinePresenter(Injection.provideTasksRepository(getActivity().getApplicationContext()), this);

        initChartKline();//??????K????????????????????????
        initChartVolume();//???????????????????????????????????????
        setChartListener();//????????????


    }
//    private Currency mCurrency ;
//    private void getCurrent(){
//        WonderfulOkhttpUtils.post().url(UrlFactory.getAllCurrencys()).build()
//                .execute(new StringCallback() {
//                    @Override
//                    public void onError(Request request, Exception e) {
//
//                    }
//
//                    @Override
//                    public void onResponse(String response) {
//                        JsonObject object = new JsonParser().parse(response).getAsJsonObject();
//                        JsonArray array = object.getAsJsonArray("changeRank").getAsJsonArray();
//                        List<Currency>  objs = gson.fromJson(array, new TypeToken<List<Currency>>() {}.getType());
//                        setCurrentcy(objs);
//                    }
//                });
//    }
//
//    private void setCurrentcy(List<Currency> objs) {
//        if(objs == null || objs.size() == 0) return;
//        for(Currency currency : objs){
//            if(symbol.equals(currency.getSymbol())){
//                mCurrency = currency;
//            }
//        }
//        mDataFour.setText(String.valueOf(mCurrency.getHigh()));
//        mDataFive.setText(String.valueOf(mCurrency.getLow()));
//        mDataThree.setText(String.valueOf(mCurrency.getVolume()));
//        mDataTwo.setText(String.valueOf(mCurrency.getChg()));
//        mDataOne.setText(String.valueOf("$" + mCurrency.getClose() + "  ???"));
//        Log.d("jiejie","mc" + mCurrency.getSymbol() + " ---" + mCurrency.getClose());
//    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public void tcpNotify(Currency mCurrency){
        try {
            mDataFour.setText(String.valueOf(mCurrency.getHigh()));
            mDataFive.setText(String.valueOf(mCurrency.getLow()));
            mDataThree.setText(String.valueOf(mCurrency.getVolume()));
            mDataTwo.setText(String.valueOf(mCurrency.getChg()));
            mDataOne.setText(String.valueOf("$" + mCurrency.getClose() + "  ???"));
            mDataText.setText("???" + WonderfulMathUtils.getRundNumber(mCurrency.getClose() * MainActivity.rate * mCurrency.getBaseUsdRate(),
                    2, null) + "CNY");
        }catch (Exception e){
            e.printStackTrace();
        }


    }
    /**
     * ??????K????????????????????????
     */
    private void initChartKline() {
        mChartKline.setScaleEnabled(true);//????????????????????????
        mChartKline.setDrawBorders(true);//??????????????????
        mChartKline.setBorderWidth(0.8f);//?????????????????????dp
        mChartKline.setDragEnabled(true);//????????????????????????
        mChartKline.setScaleYEnabled(false);//??????Y???????????????
        mChartKline.setBorderColor(Color.parseColor(color));//????????????
        mChartKline.setDescription("");//?????????????????????????????????
        mChartKline.setMinOffset(0f);
        mChartKline.setExtraOffsets(0f, 0f, 0f, 3f);

        Legend lineChartLegend = mChartKline.getLegend();
        lineChartLegend.setEnabled(false);//???????????? Legend ??????
        lineChartLegend.setForm(Legend.LegendForm.CIRCLE);

        // x y???
        xAxisKline = mChartKline.getXAxis();
        xAxisKline.setDrawLabels(true); //????????????X?????????????????????????????????true

        xAxisKline.setDrawGridLines(false);

//        xAxisKline.setDrawGridLines(false);//????????????X???????????????????????????????????????true
        xAxisKline.setDrawAxisLine(false); //?????????????????????????????????????????????????????????????????????true
        xAxisKline.enableGridDashedLine(10f, 10f, 0f);//????????????X?????????????????????(float lineLength, float spaceLength, float phase)???????????????1.?????????2.???????????????3.??????????????????
        xAxisKline.setTextColor(Color.parseColor(textColor));//??????????????????
        xAxisKline.setPosition(XAxis.XAxisPosition.BOTTOM);//??????????????????????????????
        xAxisKline.setAvoidFirstLastClipping(true);//??????????????????????????????????????????????????????
        axisLeftKline = mChartKline.getAxisLeft();
        axisLeftKline.setDrawGridLines(true);
        axisLeftKline.enableGridDashedLine(10f, 10f, 0f);
        axisLeftKline.setDrawAxisLine(false);
        axisLeftKline.setDrawZeroLine(false);
        axisLeftKline.setDrawLabels(true);
        axisLeftKline.setTextColor(Color.parseColor(textColor));
//        axisLeftKline.setGridColor(getResources().getColor(R.color.minute_grayLine));
        axisLeftKline.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        axisLeftKline.setLabelCount(4, false); //??????????????????Y??????????????????????????????????????? ????????????????????????true??????????????????
        axisLeftKline.setSpaceTop(10);//??????????????????

        axisRightKline = mChartKline.getAxisRight();
        axisRightKline.setDrawLabels(false);
        axisRightKline.setDrawGridLines(false);
        axisRightKline.setDrawAxisLine(false);
        axisRightKline.setLabelCount(4, false); //??????????????????Y??????????????????????????????????????? ????????????????????????true??????????????????

        mChartKline.setDragDecelerationEnabled(true);
        mChartKline.setDragDecelerationFrictionCoef(0.2f);

        mChartKline.animateXY(2000, 2000);
    }

    /**
     * ???????????????????????????????????????
     */
    private void initChartVolume() {
        mChartVolume.setDrawBorders(true);  //??????????????????
        mChartVolume.setBorderWidth(0.8f);//??????????????????float?????????dp??????
        mChartVolume.setBorderColor(Color.parseColor(color));//????????????
        mChartVolume.setDescription(""); //??????????????????????????????????????????String??????
        mChartVolume.setDragEnabled(true);// ??????????????????
        mChartVolume.setScaleYEnabled(false); //?????????????????? ???y???
        mChartVolume.setMinOffset(3f);
        mChartVolume.setExtraOffsets(0f, 0f, 0f, 5f);
        Legend combinedchartLegend = mChartVolume.getLegend(); // ??????????????????????????????????????????y???value???
        combinedchartLegend.setEnabled(false);//?????????????????????
        //bar x y???
        xAxisVolume = mChartVolume.getXAxis();
        xAxisVolume.setEnabled(false);
//        xAxisVolume.setDrawLabels(false); //????????????X?????????????????????????????????true
//        xAxisVolume.setDrawGridLines(false);//????????????X???????????????????????????????????????true
//        xAxisVolume.setDrawAxisLine(false); //?????????????????????????????????????????????????????????????????????true
//        xAxisVolume.enableGridDashedLine(10f, 10f, 0f);//????????????X?????????????????????(float lineLength, float spaceLength, float phase)???????????????1.?????????2.???????????????3.??????????????????
//        xAxisVolume.setTextColor(getResources().getColor(R.color.text_color_common));//??????????????????
//        xAxisVolume.setPosition(XAxis.XAxisPosition.BOTTOM);//??????????????????????????????
//        xAxisVolume.setAvoidFirstLastClipping(true);//??????????????????????????????????????????????????????

        axisLeftVolume = mChartVolume.getAxisLeft();
        axisLeftVolume.setAxisMinValue(0);//??????Y????????????????????????
//        axisLeftVolume.setShowOnlyMinMax(true);//??????Y????????????????????????
        axisLeftVolume.setDrawGridLines(true);
        axisLeftVolume.setDrawAxisLine(false);
//        axisLeftVolume.setShowOnlyMinMax(true);
        axisLeftVolume.setDrawLabels(true);
        axisLeftVolume.enableGridDashedLine(10f, 10f, 0f);
        axisLeftVolume.setTextColor(Color.parseColor(textColor));
//        axisLeftVolume.setGridColor(getResources().getColor(R.color.minute_grayLine));
        axisLeftVolume.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        axisLeftVolume.setLabelCount(1, false); //??????????????????Y??????????????????????????????????????? ????????????????????????true??????????????????
        axisLeftVolume.setSpaceTop(0);//??????????????????
//        axisLeftVolume.setSpaceBottom(0);//??????????????????

        axisRightVolume = mChartVolume.getAxisRight();
        axisRightVolume.setDrawLabels(false);
        axisRightVolume.setDrawGridLines(false);
        axisRightVolume.setDrawAxisLine(false);

        mChartVolume.setDragDecelerationEnabled(true);
        mChartVolume.setDragDecelerationFrictionCoef(0.2f);

        mChartVolume.animateXY(2000, 2000);
    }

    /**
     * ??????
     */
    private void setChartListener() {
        // ???K?????????????????????????????????????????????
        mChartKline.setOnChartGestureListener(new CoupleChartGestureListener(mChartKline, new Chart[]{mChartVolume}));
        // ??????????????????????????????????????????K?????????
        mChartVolume.setOnChartGestureListener(new CoupleChartGestureListener(mChartVolume, new Chart[]{mChartKline}));

        mChartKline.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
                Highlight highlight = new Highlight(h.getXIndex(), h.getValue(), h.getDataIndex(), h.getDataSetIndex());
                float touchY = h.getTouchY() - mChartKline.getHeight();
                Highlight h1 = mChartVolume.getHighlightByTouchPoint(h.getXIndex(), touchY);
                highlight.setTouchY(touchY);
                if (null == h1) {
                    highlight.setTouchYValue(0);
                } else {
                    highlight.setTouchYValue(h1.getTouchYValue());
                }
                mChartVolume.highlightValues(new Highlight[]{highlight});
                updateText(e.getXIndex());
            }

            @Override
            public void onNothingSelected() {
                mChartVolume.highlightValue(null);
            }
        });

        mChartVolume.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
                Highlight highlight = new Highlight(h.getXIndex(), h.getValue(), h.getDataIndex(), h.getDataSetIndex());

                float touchY = h.getTouchY() + mChartKline.getHeight();
                Highlight h1 = mChartKline.getHighlightByTouchPoint(h.getXIndex(), touchY);
                highlight.setTouchY(touchY);
                if (null == h1) {
                    highlight.setTouchYValue(0);
                } else {
                    highlight.setTouchYValue(h1.getTouchYValue());
                }
                mChartKline.highlightValues(new Highlight[]{highlight});
                updateText(e.getXIndex());
            }

            @Override
            public void onNothingSelected() {
                mChartKline.highlightValue(null);
            }
        });
    }

    /**
     * ????????????
     */
    private synchronized void updateText(int index) {
        try {
            KLineBean klData = kLineDatas.get(index);
            float change = (klData.close - klData.open) / klData.open;
            NumberFormat nf = NumberFormat.getPercentInstance();
            nf.setMaximumFractionDigits(2);
            String changePercentage = nf.format(Double.valueOf(String.valueOf(change)));
            tvKInfo.setText("??? " + MyUtils.getDecimalFormatVol(klData.open) + " ???  " + MyUtils.getDecimalFormatVol(klData.high) + " ??? " +
                    MyUtils.getDecimalFormatVol(klData.low) + " ??? " + MyUtils.getDecimalFormatVol(klData.close) + " ?????? " + changePercentage);
            int newIndex = index;
            if (newIndex < 4) {
                tvMaInfo.setText("MA5:--  MA10:" + "--" + "  MA20:" + "--");
            } else if (newIndex >= 4 && newIndex < 9) {
                tvMaInfo.setText("MA5:" + kData.getMa5DataL().get(newIndex - 4).getVal() + "  MA10:" + "--" + "  MA20:" + "--");
            } else if (newIndex >= 9 && newIndex < 19) {
                tvMaInfo.setText("MA5:" + kData.getMa5DataL().get(newIndex - 4).getVal() + "  MA10:" +
                        kData.getMa10DataL().get(newIndex - 9).getVal() + "  MA20:" + "--");
            } else {
                tvMaInfo.setText("MA5:" + kData.getMa5DataL().get(newIndex - 4).getVal() + "  MA10:" + kData.getMa10DataL().get(newIndex - 9).getVal() +
                        "  MA20:" + kData.getMa20DataL().get(newIndex - 19).getVal());
            }
        } catch (Exception ex) {
            // do nothing
        }
    }

    private void setMarkerViewButtom(DataParse mData, MyCombinedChart combinedChart) {
        try {
            MyLeftMarkerView leftMarkerView = new MyLeftMarkerView(getActivity(), R.layout.mymarkerview);
            MyHMarkerView hMarkerView = new MyHMarkerView(getActivity(), R.layout.mymarkerview_line);
            MyBottomMarkerView bottomMarkerView = new MyBottomMarkerView(getActivity(), R.layout.mymarkerview);
            combinedChart.setMarker(leftMarkerView, bottomMarkerView, hMarkerView, mData);
        } catch (Exception ex) {
            //do nothing
        }
    }

    private void setMarkerView(DataParse mData, MyCombinedChart combinedChart) {
        MyLeftMarkerView leftMarkerView = new MyLeftMarkerView(getActivity(), R.layout.mymarkerview);
        MyHMarkerView hMarkerView = new MyHMarkerView(getActivity(), R.layout.mymarkerview_line);
        combinedChart.setMarker(leftMarkerView, hMarkerView, mData);
    }

    @Override
    protected void obtainData() {

    }

    @Override
    protected void fillWidget() {

    }

    @Override
    protected void loadData() {
        Long to = System.currentTimeMillis();
        Long from = to;
        switch (type) {
            case LINE:
                return;
            case MIN1:
                from = to - 24L * 60 * 60 * 1000;//???????????????
                resolution = 1 + "";
                break;
            case MIN5:
                from = to - 2 * 24L * 60 * 60 * 1000;//???????????????
                resolution = 5 + "";
                break;
            case MIN30:
                from = to - 12 * 24L * 60 * 60 * 1000; //???12?????????
                resolution = 30 + "";
                break;
            case HOUR1:
                from = to - 24 * 24L * 60 * 60 * 1000;//??? 24?????????
                resolution = 1 + "H";
                break;
            case DAY:
                from = to - 60 * 24L * 60 * 60 * 1000; //???48?????????
                resolution = 1 + "D";
                break;
        }
        presenter.KData(symbol, from, to, resolution);
    }

    @Override
    protected boolean isImmersionBarEnabled() {
        return false;
    }

    @Override
    public void setPresenter(KlineContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void KDataFail(Integer code, String toastMessage) {

    }

    @Override
    public void KDataSuccess(JSONArray obj) {
        hideLoadingPopup();
        if (obj == null) {
            WonderfulToastUtils.showToast("???????????????");
            return;
        }
        kData = new DataParse();
        kData.parseKLine(obj);
        mCacheData = new DataParse();
        mCacheData.parseKLine(obj);
        kLineDatas = kData.getKLineDatas();
        kData.initLineDatas(kLineDatas);
        setMarkerViewButtom(kData, mChartKline);
        setMarkerView(kData, mChartVolume);
        kData.initKLineMA(kLineDatas);
        kData.initVlumeMA(kLineDatas);
        setVolumeEntryData(mChartVolume);

        setKlineEntryData(mChartKline);
        //??????
//        refreshThread = new RefreshThread();
//        refreshThread.start();

        mChartKline.moveViewToX(kLineDatas.size() - 1);
        mChartVolume.moveViewToX(kLineDatas.size() - 1);
        handler.sendEmptyMessageDelayed(0, 300);
    }

    @Override
    public void allCurrencySuccess(List<Currency> obj) {
        if (obj == null) return;
        for (Currency currency : obj) {
            if (symbol.equals(currency.getSymbol())) {
                this.currency = currency;
                break;
            }
        }
        Long to = System.currentTimeMillis();
        Date date = new Date();
        String str = WonderfulDateUtils.getFormatTime(null, date);
        String strFrom = str.split(" ")[0] + " 00:00:00";
        long from = WonderfulDateUtils.getTimeMillis(null, strFrom);
        WonderfulLogUtils.loge("INFO", "str  " + str + "****" + "from  " + from + "******" + "to  " + to);
        resolution = 1 + "";
        presenter.KData(symbol, from, to, resolution);
    }

    @Override
    public void allCurrencyFail(Integer code, String toastMessage) {
        //do nothing
    }


    private void setKlineEntryData(MyCombinedChart combinedChart) {
        //??????data
        CandleDataSet set = new CandleDataSet(kData.getCandleEntries(), "");
        set.setDrawHorizontalHighlightIndicator(false);
        set.setHighlightEnabled(true);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setShadowWidth(1f);
        set.setValueTextSize(10f);
        set.setDecreasingColor(getResources().getColor(R.color.typeRed));//???????????????????????????????????????
        set.setDecreasingPaintStyle(Paint.Style.FILL);
        set.setIncreasingColor(getResources().getColor(R.color.typeGreen));//???????????????????????????????????????
        set.setIncreasingPaintStyle(Paint.Style.FILL);
        set.setNeutralColor(getResources().getColor(R.color.typeRed));//???????????????????????????????????????
        set.setShadowColorSameAsCandle(true);
        set.setHighlightLineWidth(1f);
        set.setHighLightColor(Color.parseColor(textColor));
        set.setDrawValues(true);
        set.setValueTextColor(Color.parseColor(textColor));
        CandleData candleData = new CandleData(kData.getxVals(), set);
        //MA data
//        kData.initKLineMA(kLineDatas);
        ArrayList<ILineDataSet> sets = new ArrayList<>();
        sets.add(setMaLine(5, kData.getxVals(), kData.getMa5DataL()));
        sets.add(setMaLine(10, kData.getxVals(), kData.getMa10DataL()));
        sets.add(setMaLine(20, kData.getxVals(), kData.getMa20DataL()));
        LineData lineData = new LineData(kData.getxVals(), sets);

        CombinedData combinedData = new CombinedData(kData.getxVals());
        combinedData.setData(lineData);
        combinedData.setData(candleData);
        combinedChart.setData(combinedData);
        setHandler(combinedChart);
    }

    private void setVolumeEntryData(MyCombinedChart combinedChart) {
        String unit = MyUtils.getVolUnit(kData.getVolmax());
        String wan = "??????";
        String yi = "??????";
        int u = 0;
        if (wan.equals(unit)) {
            u = 4;
        } else if (yi.equals(unit)) {
            u = 8;
        }
        combinedChart.getAxisLeft().setValueFormatter(new VolFormatter((int) Math.pow(10, u)));
//        combinedChart.getAxisLeft().setAxisMaxValue(kData.getVolmax());

        BarDataSet set = new BarDataSet(kData.getBarEntries(), "?????????");
        set.setBarSpacePercent(5); //bar??????
        set.setHighlightEnabled(true);
        set.setHighLightAlpha(255);
        set.setHighLightColor(Color.parseColor(textColor));
        set.setDrawValues(false);

        List<Integer> list = new ArrayList<>();
        list.add(getResources().getColor(R.color.typeGreen));
        list.add(getResources().getColor(R.color.typeRed));
        set.setColors(list);
        BarData barData = new BarData(kData.getxVals(), set);

//        kData.initVlumeMA(kLineDatas);
        ArrayList<ILineDataSet> sets = new ArrayList<>();

        /******????????????????????????????????????????????????MA?????????????????????????????????0??????????????????????????????******************************/
        sets.add(setMaLine(5, kData.getxVals(), kData.getMa5DataV()));
        sets.add(setMaLine(10, kData.getxVals(), kData.getMa10DataV()));
        sets.add(setMaLine(20, kData.getxVals(), kData.getMa20DataV()));

        LineData lineData = new LineData(kData.getxVals(), sets);

        CombinedData combinedData = new CombinedData(kData.getxVals());
        combinedData.setData(barData);
        combinedData.setData(lineData);
        combinedChart.setData(combinedData);
        setHandler(combinedChart);
    }

    private void setHandler(MyCombinedChart combinedChart) {
        final ViewPortHandler viewPortHandlerBar = combinedChart.getViewPortHandler();
        float max = culcMaxscale(kData.getxVals().size());
        viewPortHandlerBar.setMaximumScaleX(max);
        Matrix touchmatrix = viewPortHandlerBar.getMatrixTouch();
        final float xscale = max / 2;
        touchmatrix.postScale(xscale, 1f);
    }

    private float culcMaxscale(float count) {
        float max = 1;
        max = count / 127 * 5;
        return max;
    }

    @NonNull
    private LineDataSet setMaLine(int ma, ArrayList<String> xVals, ArrayList<Entry> lineEntries) {
        LineDataSet lineDataSetMa = new LineDataSet(lineEntries, "ma" + ma);
        if (ma == 5) {
            lineDataSetMa.setHighlightEnabled(true);
            lineDataSetMa.setDrawHorizontalHighlightIndicator(false);
            lineDataSetMa.setHighLightColor(Color.parseColor(textColor));
        } else {/*??????????????????*/
            lineDataSetMa.setHighlightEnabled(false);
        }
        lineDataSetMa.setDrawValues(false);
        if (ma == 5) {
            lineDataSetMa.setColor(Color.parseColor("#7FB446"));
        } else if (ma == 10) {
            lineDataSetMa.setColor(Color.parseColor("#B7B910"));
        } else if (ma == 20) {
            lineDataSetMa.setColor(Color.parseColor("#884898"));
        } else {
            lineDataSetMa.setColor(Color.parseColor("#5C3F7D"));
        }
        lineDataSetMa.setLineWidth(1f);
        lineDataSetMa.setDrawCircles(false);
        lineDataSetMa.setAxisDependency(YAxis.AxisDependency.LEFT);

        lineDataSetMa.setHighlightEnabled(false);
        return lineDataSetMa;
    }

    public enum Type {
        LINE, MIN1, MIN5, MIN30, HOUR1, DAY
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    class RefreshThread extends Thread {
        @Override
        public void run() {
            while (!isNeedStop) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addData();
                        addKlineData();
                        addVolumeData();

                        mChartKline.setAutoScaleMinMaxEnabled(true);
                        mChartKline.notifyDataSetChanged();
                        mChartKline.invalidate();

                        mChartVolume.setAutoScaleMinMaxEnabled(true);
                        mChartVolume.notifyDataSetChanged();
                        mChartVolume.invalidate();
                    }
                });
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    int i = 0;

    private void addData() {
        int i = getRandom(mCacheData.getKLineDatas().size() - 1, 0);
        kLineDatas.add(kLineDatas.get(i));
        kData.getxVals().add(kLineDatas.get(i).date);
    }

    private int getRandom(int max, int min) {
        int index = i;
        i++;
        if (index > max) {
            i = 0;
            index = i;
        }
        return index;
    }

    private void addKlineData() {
        CandleData combinedData = mChartKline.getCandleData();
        LineData lineData = mChartKline.getLineData();
        int count = 0;
        int i = kLineDatas.size() - 1;
        String xVals = kData.getxVals().get(kData.getxVals().size() - 1);
        if (combinedData != null) {
            int indexLast = getLastDataSetIndex(combinedData);
            CandleDataSet lastSet = (CandleDataSet) combinedData.getDataSetByIndex(indexLast);
            if (lastSet == null) {
                lastSet = createCandleDataSet();
                combinedData.addDataSet(lastSet);
            }
            count = lastSet.getEntryCount();
            // ???????????????DataSet??????entry
            combinedData.addEntry(new CandleEntry(count, kLineDatas.get(i).high, kLineDatas.get(i).low, kLineDatas.get(i).open, kLineDatas.get(i).close), indexLast);
        }

        if (lineData != null) {
            int index = getDataSetIndexCount(lineData);
            LineDataSet lineDataSet5 = (LineDataSet) lineData.getDataSetByIndex(0);//????????????;
            LineDataSet lineDataSet10 = (LineDataSet) lineData.getDataSetByIndex(1);//????????????;
            LineDataSet lineDataSet20 = (LineDataSet) lineData.getDataSetByIndex(2);//???????????????;
            LineDataSet lineDataSet30 = (LineDataSet) lineData.getDataSetByIndex(3);//???????????????;

            if (lineDataSet5 != null) {
                kData.getMa5DataL().add(new Entry(KMAEntity.getLastMA(kLineDatas, 5), count));
                lineData.addEntry(kData.getMa5DataL().get(kData.getMa5DataL().size() - 1), 0);
            }

            if (lineDataSet10 != null) {
                kData.getMa10DataL().add(new Entry(KMAEntity.getLastMA(kLineDatas, 10), count));
                lineData.addEntry(kData.getMa10DataL().get(kData.getMa10DataL().size() - 1), 1);
            }

            if (lineDataSet20 != null) {
                kData.getMa20DataL().add(new Entry(KMAEntity.getLastMA(kLineDatas, 20), count));
                lineData.addEntry(kData.getMa20DataL().get(kData.getMa20DataL().size() - 1), 2);
            }

            if (lineDataSet30 != null) {
                kData.getMa30DataL().add(new Entry(KMAEntity.getLastMA(kLineDatas, 30), count));
                lineData.addEntry(kData.getMa30DataL().get(kData.getMa30DataL().size() - 1), 3);
            }
        }
    }

    /**
     * ??????????????????CandleDataSet?????????
     */
    private int getLastDataSetIndex(CandleData candleData) {
        int dataSetCount = candleData.getDataSetCount();
        return dataSetCount > 0 ? (dataSetCount - 1) : 0;
    }

    /**
     * ??????????????????LineDataSet?????????
     */
    private int getDataSetIndexCount(LineData lineData) {
        int dataSetCount = lineData.getDataSetCount();
        return dataSetCount;
    }

    private CandleDataSet createCandleDataSet() {
        CandleDataSet dataSet = new CandleDataSet(null, "DataSet 1");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setValueTextSize(12f);
        return dataSet;
    }

    private void addVolumeData() {
        BarData barData = mChartVolume.getBarData();
        LineData lineData = mChartVolume.getLineData();
        int count = 0;
        int i = kLineDatas.size() - 1;
        String xVals = kData.getxVals().get(kData.getxVals().size() - 1);
        if (barData != null) {
            int indexLast = getLastDataSetIndex(barData);
            BarDataSet lastSet = (BarDataSet) barData.getDataSetByIndex(indexLast);
            if (lastSet == null) {
                lastSet = createBarDataSet();
                barData.addDataSet(lastSet);
            }
            count = lastSet.getEntryCount();

//            barData.addXValue(xVals);
            // ???????????????DataSet??????entry
            barData.addEntry(new BarEntry(count, kLineDatas.get(i).high, kLineDatas.get(i).low, kLineDatas.get(i).open, kLineDatas.get(i).close, kLineDatas.get(i).vol), indexLast);
        }

        if (lineData != null) {
            int index = getDataSetIndexCount(lineData);
            LineDataSet lineDataSet5 = (LineDataSet) lineData.getDataSetByIndex(0);//????????????;
            LineDataSet lineDataSet10 = (LineDataSet) lineData.getDataSetByIndex(1);//????????????;
            LineDataSet lineDataSet20 = (LineDataSet) lineData.getDataSetByIndex(2);//???????????????;
            LineDataSet lineDataSet30 = (LineDataSet) lineData.getDataSetByIndex(3);//???????????????;

            if (lineDataSet5 != null) {
                kData.getMa5DataV().add(new Entry(VMAEntity.getLastMA(kLineDatas, 5), count));
                lineData.addEntry(kData.getMa5DataV().get(kData.getMa5DataV().size() - 1), 0);
            }

            if (lineDataSet10 != null) {
                kData.getMa10DataV().add(new Entry(VMAEntity.getLastMA(kLineDatas, 10), count));
                lineData.addEntry(kData.getMa10DataV().get(kData.getMa10DataV().size() - 1), 1);
            }

            if (lineDataSet20 != null) {
                kData.getMa20DataV().add(new Entry(VMAEntity.getLastMA(kLineDatas, 20), count));
                lineData.addEntry(kData.getMa20DataV().get(kData.getMa20DataV().size() - 1), 2);
            }

            if (lineDataSet30 != null) {
                kData.getMa30DataV().add(new Entry(VMAEntity.getLastMA(kLineDatas, 30), count));
                lineData.addEntry(kData.getMa30DataV().get(kData.getMa30DataV().size() - 1), 3);
            }
        }
    }

    /**
     * ??????????????????DataSet?????????
     */
    private int getLastDataSetIndex(BarData barData) {
        int dataSetCount = barData.getDataSetCount();
        return dataSetCount > 0 ? (dataSetCount - 1) : 0;
    }

    private BarDataSet createBarDataSet() {
        BarDataSet dataSet = new BarDataSet(null, "DataSet 1");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setValueTextSize(12f);

        return dataSet;
    }
}
