package com.gotosea.other_demo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.*;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnGetGeoCoderResultListener{

    //百度Key
    private static final String BAIDU_MAP_KEY = "1g2qTchTvgaxm8nEKOxFA9sfrhb1uMGx";
    //地图视图
    private MapView mMapView = null;
    private LocationClient mLocationClient = null;
    //是否是第一次定位
    private volatile boolean isFirstLocation = true;
    //最新一次的纬度
    private double mCurrentLatitude;
    //最新一次的经度
    private double mCurrentLongitude;
    //当前的经纬度
    private float mCurrentLL;
    //方向传感器的监听器
    private MyOrientationListener myOrientationListener;
    //方向传感器X方向的值
    private int mXDirection;
    private BaiduMap mBaiduMap;
    //我的定位监听器
    public MyLocationListener mMyLocationListener;
    private LocationMode mCurrentMode = LocationMode.NORMAL;
    //地图定位的模式
    private String[] mStyles = new String[] {"地图模式【正常】", "地图模式【跟随】",
            "地图模式【罗盘】"};
    private int mCurrentStyle = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            requestContactPermission();
        }else {
        }
        initMyLocation();
        initOrientationListener();
    }

    /**
     * 系统大于6.0的动态获取权限
     */
    private void requestContactPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        List<String> permissionsList = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++){
            if (ActivityCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permissions[i]);
            }
        }
        if (permissionsList.size() > 0){
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), 123);
        }else {
            onPermission(new String[]{});
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 123:
                List<String> denied = new ArrayList<>();
                for (int i = 0; i < permissions.length; i++){
                    if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                        denied.add(permissions[i]);
                    }
                }
                onPermission(denied.toArray(new String[denied.size()]));
                break;
        }
    }

    private void onPermission(String[] strings) {
        if (strings.length > 0){
            Toast.makeText(this, "访问地图需要获取定位权限！",Toast.LENGTH_LONG).show();
            return;
        }
        initMyLocation();
        initOrientationListener();
    }

    /**
     * 初始化定位
     */
    private void initMyLocation() {
        //获取地图控件引用
        mMapView =  findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        isFirstLocation = true;
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);
        mBaiduMap.setMapStatus(msu);
        // 定位初始化
        mLocationClient = new LocationClient(this);
        mMyLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(mMyLocationListener);
        // 开启图层定位
        mBaiduMap.setMyLocationEnabled(true);
        if (!mLocationClient.isStarted()){
            mLocationClient.start();
        }
        // 设置定位的相关配置
        LocationClientOption option = new LocationClientOption();
        // 打开gps
        option.setOpenGps(true);
        option.setIsNeedAddress(true);
        // 设置坐标类型
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        mLocationClient.setLocOption(option);
    }

    /**
     * 初始化方向传感器
     */
    private void initOrientationListener() {
        myOrientationListener = new MyOrientationListener(getApplicationContext());
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener(){
                    @Override
                    public void onOrientationChanged(float x){
                        mXDirection = (int) x;
                        // 构造定位数据
                        MyLocationData locData = new MyLocationData.Builder()
                                .accuracy(mCurrentLL)
                                // 此处设置开发者获取到的方向信息，顺时针0-360
                                .direction(mXDirection)
                                .latitude(mCurrentLatitude)
                                .longitude(mCurrentLongitude).build();
                        // 设置定位数据
                        mBaiduMap.setMyLocationData(locData);
                        // 设置自定义图标
                        BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromResource(R.mipmap.navi_map_gps_locked);
                        MyLocationConfiguration config = new MyLocationConfiguration(mCurrentMode, true, mCurrentMarker);
                        mBaiduMap.setMyLocationConfigeration(config);

                    }
                });
        // 开启方向传感器
        myOrientationListener.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 开启图层定位
        mBaiduMap.setMyLocationEnabled(true);
        if (!mLocationClient.isStarted()){
            mLocationClient.start();
        }
        myOrientationListener.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 关闭图层定位
        mBaiduMap.setMyLocationEnabled(false);
        mLocationClient.stop();
        // 关闭方向传感器
        myOrientationListener.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mMapView = null;
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL && menu != null){
            if (menu.getClass().getSimpleName().equals("MenuBuilder")){
                try{
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.id_menu_map_common:
                // 普通地图
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                break;
            case R.id.id_menu_map_site:// 卫星地图
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.id_menu_map_traffic:
                // 开启交通图
                if (mBaiduMap.isTrafficEnabled()){
                    item.setTitle("开启实时交通");
                    mBaiduMap.setTrafficEnabled(false);
                } else{
                    item.setTitle("关闭实时交通");
                    mBaiduMap.setTrafficEnabled(true);
                }
                break;
            case R.id.id_menu_map_myLoc:
                center2myLoc();
                break;
            case R.id.id_menu_map_style:
                mCurrentStyle = (++mCurrentStyle) % mStyles.length;
                item.setTitle(mStyles[mCurrentStyle]);
                // 设置自定义图标
                switch (mCurrentStyle){
                    case 0:
                        mCurrentMode = LocationMode.NORMAL;
                        break;
                    case 1:
                        mCurrentMode = LocationMode.FOLLOWING;
                        break;
                    case 2:
                        mCurrentMode = LocationMode.COMPASS;
                        break;
                }
                BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromResource(R.mipmap.navi_map_gps_locked);
                MyLocationConfiguration config = new MyLocationConfiguration( mCurrentMode, true, mCurrentMarker);
                mBaiduMap.setMyLocationConfigeration(config);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * 地图移动到我的位置,此处可以重新发定位请求，然后定位；
     * 直接拿最近一次经纬度，如果长时间没有定位成功，可能会显示效果不好
     */
    private void center2myLoc(){
        LatLng ll = new LatLng(mCurrentLatitude, mCurrentLongitude);
        MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
        mBaiduMap.animateMapStatus(u);
    }

    public class MyLocationListener implements BDLocationListener {

       @Override
       public void onReceiveLocation(BDLocation bdLocation) {
           if (bdLocation == null || mMapView == null){
               return;
           }
           MyLocationData locData = new MyLocationData.Builder()
                   .accuracy(bdLocation.getRadius())
                   // 此处设置开发者获取到的方向信息，顺时针0-360
                   .direction(mXDirection)
                   .latitude(bdLocation.getLatitude())
                   .longitude(bdLocation.getLongitude())
                   .build();
           mCurrentLL = bdLocation.getRadius();
           String city = bdLocation.getAddrStr();
           int locType = bdLocation.getLocType();
           Log.i("MyLocationListener", "onReceiveLocation: 城市：" + city + "；返回状态：" + locType);
           // 设置定位数据
           mBaiduMap.setMyLocationData(locData);
           mCurrentLatitude = bdLocation.getLatitude();
           mCurrentLongitude = bdLocation.getLongitude();
           Log.i("MyLocationListener", "onReceiveLocation: 纬度3：" + mCurrentLatitude + "；经度3：" +mCurrentLongitude + "；当前定位点：" + mCurrentLL);
           // 设置自定义图标
           BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromResource(R.mipmap.navi_map_gps_locked);
           MyLocationConfiguration config = new MyLocationConfiguration(mCurrentMode, true, mCurrentMarker);
           mBaiduMap.setMyLocationConfigeration(config);
           // 第一次定位时，将地图位置移动到当前位置
           if (isFirstLocation){
               isFirstLocation = false;
               LatLng ll = new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
               MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
               mBaiduMap.animateMapStatus(u);
           }
       }
   }

}
