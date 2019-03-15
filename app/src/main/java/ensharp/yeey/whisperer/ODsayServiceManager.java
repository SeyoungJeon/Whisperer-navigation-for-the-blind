package ensharp.yeey.whisperer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.odsay.odsayandroidsdk.API;
import com.odsay.odsayandroidsdk.ODsayData;
import com.odsay.odsayandroidsdk.ODsayService;
import com.odsay.odsayandroidsdk.OnResultCallbackListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

import ensharp.yeey.whisperer.Common.VO.CloserStationVO;
import ensharp.yeey.whisperer.Common.ParseManager;
import ensharp.yeey.whisperer.Common.VO.BusStopVO;
import ensharp.yeey.whisperer.Common.VO.BusVO;
import ensharp.yeey.whisperer.Common.VO.DefaultInfoVO;
import ensharp.yeey.whisperer.Common.VO.ExchangeInfoVO;
import ensharp.yeey.whisperer.Common.VO.ExitInfoVO;
import ensharp.yeey.whisperer.Common.VO.PathVO;
import ensharp.yeey.whisperer.Common.VO.StationVO;
import ensharp.yeey.whisperer.Common.VO.SubwayStationInfoVO;
import ensharp.yeey.whisperer.Common.VO.SubwayTimeTableVO;
import ensharp.yeey.whisperer.Common.VO.UseInfoVO;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import static android.support.v4.content.ContextCompat.startActivity;

class ODsayServiceManager {
    private static final ODsayServiceManager ourInstance = new ODsayServiceManager();

    static ODsayServiceManager getInstance() {
        return ourInstance;
    }

    private MainActivity mainActivity;

    private ODsayService odsayService;
    private JSONObject jsonObject;
    private ParseManager parseManager;

    private PathVO path;
    private CloserStationVO closerStation;

    private Context context;
    private SubwayStationInfoVO station;
    private SubwayTimeTableVO timeTable;

    private String wayCode;

    private static String TAG = "API Callback";

    private ODsayServiceManager() {
        parseManager = ParseManager.getInstance();
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }
//    public void setContext(Context _context) { this.context = _context; }

    /**
     * 지하철 운행정보를 가져오는 API를 호출합니다.
     * ODsayService 객체는 싱글톤으로 생성됩니다.
     */
    public void initAPI(Context _context) {
        odsayService = ODsayService.init(mainActivity, mainActivity.getString(R.string.odsay_key));
        odsayService.setReadTimeout(5000);
        odsayService.setConnectionTimeout(5000);
        this.context = _context;
    }

    /**
     * API가 호출된 후 실행되는 콜백 메소드입니다.
     * 호출 결과를 로그값으로 나타냅니다.
     */
    private OnResultCallbackListener onResultCallbackListener = new OnResultCallbackListener() {
        @Override
        public void onSuccess(ODsayData oDsayData, API api) {
            jsonObject = oDsayData.getJson();

//            Log.e("jsonObject",String.valueOf(jsonObject));

            switch (api.name()) {
                case "SUBWAY_PATH": // 지하철 경로 검색
                    path = parseManager.parsePath(jsonObject);
                    // path 이용 메소드 올 곳
                    ((TextView)mainActivity.findViewById(R.id.result)).setText(path.toString());
                    break;
                case "POINT_SEARCH":
                    //closerStation = parseCloserStation(jsonObject);
                    CallStation(closerStation.getCloserStationList().get(Constant.MOST_CLOSER_STATION));
                    break;
                case "SUBWAY_STATION_INFO": // 지하철역 세부 정보
                    station = parseManager.parseStation(jsonObject);
                    // station 이용 메소드 올 곳
                    ((TextView)mainActivity.findViewById(R.id.result)).setText(station.toString());
                        break;
                case "SUBWAY_TIME_TABLE":
                    timeTable = parseManager.parseTimeTable(jsonObject, wayCode);
                    // timeTable 이용 메소드 올 곳
                    ((TextView)mainActivity.findViewById(R.id.result)).setText(timeTable.toString());
                    break;
                default:
                    Log.e(TAG, "api 이름: " + api.name());
                    break;
            }

            Log.e(TAG, "onSuccess: " + jsonObject.toString());
        }

        @Override
        public void onError(int i, String errorMessage, API api) {
            Log.e(TAG, "onError: API : " + api.name() + "\n" + errorMessage);
        }
    };

    //가까운 지하철역 코드 조회
    public void findCloserStationCode(double latitude, double longitude){
        odsayService.requestPointSearch(String.valueOf(latitude), String.valueOf(longitude), "5000", "2", onResultCallbackListener);
    }

    /**
     * 출발역과 도착역의 코드를 파라미터로 전달하면 이동 경로를 계산합니다.
     * requestSubwayPath는 비동기로 진행됩니다.
     * 계산된 이동 경로는 path에 저장됩니다.
     * @param start 출발역 코드
     * @param end 도착역 코드
     */
    public void calculatePath(String start, String end) {
        odsayService.requestSubwayPath("1000", start, end, "2", onResultCallbackListener);
    }

    /**
     * 지하철역의 세부 정보를 가져오는 메소드입니다.
     * 정보는 station에 저장됩니다.
     * @param station 지하철역 코드
     */
    //예진이 처리
//    public PathVO parsePath(JSONObject jsonObject) {
//        Gson gson = new GsonBuilder()
//                .registerTypeAdapter(PathVO.class, new RestDeserializer<>(PathVO.class, "result"))
//                .create();
//        PathVO path = gson.fromJson(jsonObject.toString(), PathVO.class);
//
//        if (path.getExChangeInfoSet() == null)
//            return path;
//
//        Log.e("1","1");
//
//        path.setExchangeInfoList(parseExchangeInfo(path.getExChangeInfoSet()));
//
//        return path;
//    }

    /**
     * 가까운 지하철 정보를 파싱하는 메소드입니다.
     * @param jsonObject API에서 반환된 JSONObject
     * @return 파싱된 PathVO 객체
     */
    //예진이 처리
//    public CloserStationVO parseCloserStation(JSONObject jsonObject) {
//        Gson gson = new GsonBuilder()
//                .registerTypeAdapter(CloserStationVO.class, new RestDeserializer<>(CloserStationVO.class, "result"))
//                .create();
//
//        CloserStationVO closerStationVO = gson.fromJson(jsonObject.toString(), CloserStationVO.class);
//
//        closerStationVO.setCloserStationList(parseStation(closerStationVO.getStation()));
//
//        return closerStationVO;
//    }

    /**
     * 가까운 지하철 정보 순으로 파싱하는 메소드입니다.
     * @param jsonObject 가까운 역 정보를 담고있는 jsonObject
     * @return 파싱된 ExchangeInfoVO List
     */
    public List<StationVO> parseStation(JsonElement jsonObject) {
        Gson gson = new Gson();

        Type listType = new TypeToken<List<StationVO>>() {}.getType();

        List<StationVO> stationList = (List<StationVO>) gson.fromJson(jsonObject, listType);

        return stationList;
    }

    //해당 역 코드로 전화번호를 찾아서 전화 걸기
    public void CallStation(StationVO mCloserStation){

        ExcelManager excelManager = new ExcelManager();
        excelManager.setContext(context);
        String stationNumber = excelManager.Find_Data(String.valueOf(mCloserStation.getStationID())
                , Constant.STATION_CODE, Constant.STATION_NUMBER);
        Log.e("stationNumber",stationNumber);
        Uri call = Uri.parse("tel:" + stationNumber);

        Intent call_intent = new Intent(Intent.ACTION_CALL, call);

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED){
            startActivity(context, call_intent,null);
        }
    }

    /**
     * 지하철 환승 정보를 파싱하는 메소드입니다.
     * @param jsonObject 환승 정보를 담고있는 jsonObject
     * @return 파싱된 ExchangeInfoVO List
     */
    public List<ExchangeInfoVO> parseExchangeInfo(JsonElement jsonObject) {
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonElement rootElement = parser.parse(jsonObject.toString())
                .getAsJsonObject().get("exChangeInfo");
        Type listType = new TypeToken<List<ExchangeInfoVO>>() {
        }.getType();
        List<ExchangeInfoVO> exchangeInfoList = (List<ExchangeInfoVO>) gson.fromJson(rootElement, listType);

        return exchangeInfoList;
    }

    public void getSubwayInfo(String station) {
        odsayService.requestSubwayStationInfo(station, onResultCallbackListener);
    }



    /**
     * 지하철역의 시간표를 가져오는 메소드입니다.
     * 정보는 timeTable에 저장됩니다.
     * @param station 지하철역 코드
     * @param wayCode 상행/하행 여부
     */
    public void getSubwayTimeTable(String station, String wayCode) {
        this.wayCode = wayCode;
        odsayService.requestSubwayTimeTable(station, wayCode, onResultCallbackListener);
    }

    /**
     * 지하철역의 이름을 입력하면 해당 역의 코드를 반환하는 메소드입니다.
     * @param station 역명
     * @return 지하철역 코드
     * String station_code = excelManager.Find_Data(station, Constant.STATION_NAME, Constant.STATION_CODE);
     */
    public String getStationCode(String station) {
        InputStream inputStream = null;
        Workbook workbook = null;
        Sheet sheet = null;
        try {
            inputStream = mainActivity.getAssets().open("station_data.xls");
            workbook = Workbook.getWorkbook(inputStream);
            sheet = workbook.getSheet(0);

            int rowStart = 1;
            int rowEnd = sheet.getColumn(0).length;
            int nameColumn = 1;
            int codeColumn = 2;

            for(int row = rowStart; row <= rowEnd; row++) {
                String name = sheet.getCell(nameColumn, row).getContents();
                if(name.equals(station)){
                    return sheet.getCell(codeColumn, row).getContents();
                }
            }

            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } catch (BiffException e) {
            e.printStackTrace();
            return "";
        } finally {
            workbook.close();
        }
    }
}
