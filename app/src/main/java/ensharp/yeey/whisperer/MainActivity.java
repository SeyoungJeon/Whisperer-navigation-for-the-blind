package ensharp.yeey.whisperer;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IARegion;

public class MainActivity extends AppCompatActivity {

    private ODsayServiceManager oDsayServiceManager;

    //indooratlas
    IALocationManager mIALocationManager;
    IALocationListener mIALocationListener;
    IARegion.Listener mRegionListener;

    String TAG = "INDOOR_ATLAS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSubwayAPI();

        int MyVersion = Build.VERSION.SDK_INT;

        //버전 체크
        if (MyVersion >= Build.VERSION_CODES.O) {
            checkVerify();
        } else {
            Toast.makeText(this, "버전이 맞지 않아 이 앱을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
            this.finish();
        }
//        oDsayServiceManager.findCloserStationCode(126.933361407195,37.3643392278118);
    }

    /**
     * 지하철 운행정보를 가져오는 API를 호출하고 초기화합니다.
     */
    private void initSubwayAPI() {
        oDsayServiceManager = ODsayServiceManager.getInstance();
        oDsayServiceManager.setMainActivity(this);
        oDsayServiceManager.initAPI(this);
    }

    //권학 확인하는 함수
    public void checkVerify()
    {
        //권한
        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CALL_PHONE
        };

        //권한 확인
        if (checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
        {
            //권한 여러개 중 하나라도 허용 못했다면?
            if (shouldShowRequestPermissionRationale(Manifest.permission.CHANGE_WIFI_STATE) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_WIFI_STATE) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE))
            {
                //권한이 왜 필요한지 설명
                // ...
            }
            requestPermissions(neededPermissions,Constant.PERMISSION_REQUEST_CODE);
        }
        else
        //권한 여러개 모두 허용 했다면?
        {
            Toast.makeText(this,"모두 허용",Toast.LENGTH_LONG).show();
        }
    }

    //권한 요청 작업에 대한 CallBack 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constant.PERMISSION_REQUEST_CODE)
        {
            if (grantResults.length > 0)
            {
                for (int i=0; i<grantResults.length; ++i)
                {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                    {
                        // 하나라도 거부한다면.
                        new AlertDialog.Builder(this).setTitle("알림").setMessage("권한을 허용해주셔야 앱을 이용할 수 있습니다.")
                                .setPositiveButton("종료", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).setNegativeButton("권한 설정", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                getApplicationContext().startActivity(intent);
                            }
                        }).setCancelable(false).show();

                        return;
                    }
                }
            }
        }
    }
}
