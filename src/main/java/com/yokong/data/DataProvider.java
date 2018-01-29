package com.yokong.data;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.region.Region;

import java.net.URL;
import java.util.*;

public class DataProvider {
    private static final String ACCESS_KEY = "AKIDoPmvqapLSKwHNHkKSKwHMBk1kIdkQFeo";
    private static final String SECRET_KEY = "3rSovnRq0wd5IFwNYT9gBSVJtELTUVQH";
    private static final String BUCKET_NAME = "yokonghe-1255950575";
    private static final String REGION_NAME = "ap-guangzhou";
    private static final String CDN_URL = "https://yokonghe-1255950575.file.myqcloud.com/";

    private COSClient mCosClient;
    private List<PhotoUrl> mPhotoList;


    private static DataProvider sInstance;

    private DataProvider() {
        initCOS();
        refreshDataFromCOS();
        initTimer();
    }

    public static DataProvider getInstance() {
        if (sInstance == null) {
            synchronized (DataProvider.class) {
                sInstance = new DataProvider();
            }
        }
        return sInstance;
    }

    private void initCOS() {
        COSCredentials cred = new BasicCOSCredentials(ACCESS_KEY, SECRET_KEY);
        // 2 设置bucket的区域, COS地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(REGION_NAME));
        // 3 生成cos客户端
        mCosClient = new COSClient(cred, clientConfig);
        // bucket的命名规则为{name}-{appid} ，此处填写的存储桶名称必须为此格式
    }

    private void initTimer() {
        Timer timer = new Timer();
        long delay = 23 * 60 * 60 * 1000;
        timer.schedule(new RefreshTimerTask(), delay, delay);
    }

    public void refreshDataFromCOS() {
        // 获取 bucket 下成员（设置 delimiter）
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.setBucketName(BUCKET_NAME);
        // 设置 list 的 prefix, 表示 list 出来的文件 key 都是以这个 prefix 开始
        listObjectsRequest.setPrefix("");
        // 设置 delimiter 为/, 即获取的是直接成员，不包含目录下的递归子成员
        listObjectsRequest.setDelimiter("/");
        // 设置 marker, (marker 由上一次 list 获取到, 或者第一次 list marker 为空)
        listObjectsRequest.setMarker("");
        // 设置最多 list 100 个成员,（如果不设置, 默认为 1000 个，最大允许一次 list 1000 个 key）
        listObjectsRequest.setMaxKeys(1000);

        ObjectListing objectListing = mCosClient.listObjects(listObjectsRequest);
        // 获取下次 list 的 marker
        String nextMarker = objectListing.getNextMarker();
        // 判断是否已经 list 完, 如果 list 结束, 则 isTruncated 为 false, 否则为 true
        boolean isTruncated = objectListing.isTruncated();
        List<COSObjectSummary> objectSummaries = objectListing.getObjectSummaries();
        if (objectSummaries == null || objectSummaries.size() == 0) {
            return;
        }
        Collections.sort(objectSummaries, new Comparator<COSObjectSummary>() {
            public int compare(COSObjectSummary o1, COSObjectSummary o2) {
                return o2.getLastModified().compareTo(o1.getLastModified());
            }
        });
        synchronized (DataProvider.class) {
            mPhotoList = new ArrayList<PhotoUrl>();
            for (COSObjectSummary cosObjectSummary : objectSummaries) {
                // 文件路径
                PhotoUrl photoUrl = new PhotoUrl();
                String key = cosObjectSummary.getKey();
                // GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(BUCKET_NAME, key,
                // HttpMethodName.GET);
                // // 设置签名过期时间(可选), 最大允许设置签名一个月有效, 若未进行设置, 则默认使用ClientConfig中的签名过期时间(5分钟)
                // // 这里设置签名在半个小时后过期
                // Date expirationDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                // req.setExpiration(expirationDate);
                // URL url = mCosClient.generatePresignedUrl(req);

                photoUrl.url = CDN_URL + key;
                photoUrl.thumbUrl = CDN_URL + "thumb/thumb-" + key;
                mPhotoList.add(photoUrl);
            }
        }
    }

    /**
     * 獲取圖片列表
     * 
     * @param sort 分頁,每頁十個圖片,第一次請求傳入0
     * @return 圖片列表和信息
     */
    public PhotoList getPhotoList(int sort) {
        List<PhotoUrl> allList = mPhotoList;
        int size = allList.size();
        if (sort * 10 > size) {
            return null;
        }
        PhotoList photoList = new PhotoList();
        List<PhotoUrl> photos;
        boolean isEnd;
        if ((sort + 1) * 10 >= size) {
            photos = allList.subList(sort * 10, allList.size());
            isEnd = true;
        } else {
            photos = allList.subList(sort * 10, (sort + 1) * 10);
            isEnd = false;
        }
        photoList.photos = photos;
        photoList.sort = ++sort;
        photoList.isEnd = isEnd;
        return photoList;
    }

    public class PhotoList {
        List<PhotoUrl> photos;
        int sort;
        boolean isEnd;
    }

    public class PhotoUrl {
        String thumbUrl;
        String url;
    }

    class RefreshTimerTask extends TimerTask {
        @Override
        public void run() {
            refreshDataFromCOS();
        }
    }

}
