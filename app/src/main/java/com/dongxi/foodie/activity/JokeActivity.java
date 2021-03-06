package com.dongxi.foodie.activity;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.dongxi.foodie.R;
import com.dongxi.foodie.adapter.JokeAdapter;
import com.dongxi.foodie.bean.JokeInfo;
import com.dongxi.foodie.view.DividerItemDecoration;
import com.nguyenhoanglam.progresslayout.ProgressLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

import static com.dongxi.foodie.R.id.swipe_layout;

public class JokeActivity extends AppCompatActivity {

    private RecyclerView lv_joke;
    private SwipeRefreshLayout swipelayout;
    List<JokeInfo> jokeInfos = new ArrayList<JokeInfo>();
    private JokeInfo jokenfo;
    private ProgressBar pb_progress;
    private JokeAdapter jokeAdapter;

    private LinearLayoutManager linearLayoutManager;
    private int lastVisibleItem;
    private int pageSize = 10;
    private int page = 1;
    private List<Integer> skipIds;
    private ProgressLayout progressLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joke);

        swipelayout = (SwipeRefreshLayout) findViewById(swipe_layout);
        lv_joke = (RecyclerView) findViewById(R.id.lv_joke);

        progressLayout = (ProgressLayout) findViewById(R.id.progressLayout);
        pb_progress = (ProgressBar) findViewById(R.id.pb_progress);

        linearLayoutManager = new LinearLayoutManager(this);
        lv_joke.setLayoutManager(linearLayoutManager);
        //设置adapter
        jokeAdapter = new JokeAdapter(jokeInfos);
        lv_joke.setAdapter(jokeAdapter);

        //配置RecyclerView 可以提高执行效率, 前提你要知道有多少不变的item
        lv_joke.setHasFixedSize(true);

        //设置item之间的间隔,分割线等
        lv_joke.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL_LIST));

        //设置进度条的背景颜色主题
        swipelayout.setProgressBackgroundColorSchemeResource(android.R.color.white);
        //设置进度条的颜色主题
        swipelayout.setColorSchemeResources(android.R.color.holo_blue_light,
                android.R.color.holo_red_light,android.R.color.holo_orange_light,
                android.R.color.holo_green_light);
        /*第一个参数scale就是就是刷新那个圆形进度是是否缩放,如果为true表示缩放,圆形进度图像就会从小到大展示出来,为false就不缩放
          第二个参数start和end就是那刷新进度条展示的相对于默认的展示位置,start和end组成一个范围，
            在这个y轴范围就是那个圆形进度ProgressView展示的位置
        // 这句话是为了，第一次进入页面的时候显示加载进度条
        */
        swipelayout.setProgressViewOffset(false, 0, (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources()
                        .getDisplayMetrics()));

        //滑动监听
        lv_joke.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView,
                                             int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && lastVisibleItem + 1 == jokeAdapter.getItemCount()) {
                    swipelayout.setRefreshing(true);
                    // 此处在现实项目中，请换成网络请求数据代码，sendRequest .....

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            page++;
                            getDataFromServer();
                            swipelayout.setRefreshing(false);
                            jokeAdapter.notifyDataSetChanged();
                            Snackbar.make(swipelayout,"刷新成功",Snackbar.LENGTH_LONG).show();
                        }
                    }, 2000);
                }
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
            }
        });
        //下拉刷新操作
        swipelayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        page++;
                        swipelayout.setRefreshing(false);
                        jokeAdapter.notifyDataSetChanged();
                    }
                }, 2000);
            }
        });


        skipIds = new ArrayList<>();
        skipIds.add(R.id.pb_progress);
        progressLayout.showLoading(skipIds);
        getDataFromServer();

    }

    /**
     * 从服务器获取数据
     */
    private void getDataFromServer() {

        pb_progress.setVisibility(View.VISIBLE);
        //http://japi.juhe.cn/joke/content/text.from?key=506689e288bb5d581a295ad76b3ebb6e&page=1&pagesize=10
        RequestParams params = new RequestParams("http://japi.juhe.cn/joke/content/text.from?key=506689e288bb5d581a295ad76b3ebb6e&" +
                "page="+String.valueOf(page)+"&pagesize="+ String.valueOf(pageSize));
        //params.setSslSocketFactory(...); // 设置ssl
        params.addQueryStringParameter("wd", "xUtils");
        x.http().get(params, new Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                parseData(result);//解析数据
                jokeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                progressLayout.showError(ContextCompat.getDrawable(JokeActivity.this, R.drawable.ic_no_connection), "No connection", "RETRY", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(JokeActivity.this, "Reloading...", Toast.LENGTH_SHORT).show();
                    }
                }, skipIds);
            }

            @Override
            public void onCancelled(CancelledException cex) {
                progressLayout.showEmpty(ContextCompat.getDrawable(JokeActivity.this, R.drawable.ic_empty), "Empty data",skipIds);
            }

            @Override
            public void onFinished() {
                progressLayout.setVisibility(View.GONE);
                swipelayout.setVisibility(View.VISIBLE);
            }
        });

    }

    /**
     * 解析网络数据
     *
     * @param result
     */
    private void parseData(String result) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(result);
            JSONObject jsonResult = jsonObject.getJSONObject("result");
            JSONArray jsonArray = jsonResult.getJSONArray("data");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                String content = jsonObj.getString("content").replaceAll("\\s*", "");//去除内容中的空格
                String updatetime = jsonObj.getString("updatetime");
                JokeInfo info = new JokeInfo(content, updatetime);
                jokeInfos.add(info);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}