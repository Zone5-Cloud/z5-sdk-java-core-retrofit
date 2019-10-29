package com.zone5ventures.retrofit.core.apis;

import java.io.File;

import com.zone5ventures.core.activities.DataFileUploadIndex;
import com.zone5ventures.core.routes.Routes;
import com.zone5ventures.core.routes.UserRoute;
import com.zone5ventures.core.routes.UserRouteOutputType;
import com.zone5ventures.core.routes.UserRouteSearch;
import com.zone5ventures.core.search.SearchResult;
import com.zone5ventures.core.utils.GsonManager;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import rx.Observable;

public interface RoutesAPI {
	
	/** Search for routes */
	@POST(Routes.SEARCH)
    Observable<SearchResult<UserRoute>> search(@Path("offset") int offset, @Path("count") int count, @Body UserRouteSearch criteria);	
	
    /** Get the next paginated batch - relative to the previous search query */
    @GET(Routes.NEXT)
    Observable<SearchResult<UserRoute>> next(@Path("offset") int offset, @Path("count") int count);
    
    /** Get summary information about the route */
    @GET(Routes.SUMMARY)
    Observable<UserRoute> summary(@Path("routeId") long routeId);
    
    /** Get summary information about the route */
    @GET(Routes.SUMMARY)
    Observable<UserRoute> summary(@Path("routeId") String uuid);
    
    /** Get detailed information about the route */
    @GET(Routes.DETAILED)
    Observable<UserRoute> detailed(@Path("routeId") long routeId);
    
    /** Get detailed information about the route */
    @GET(Routes.DETAILED)
    Observable<UserRoute> detailed(@Path("routeId") String uuid);
    
    /** Delete a route */
    @GET(Routes.DELETE)
    Observable<Boolean> delete(@Path("routeId") long routeId);
    
    /** Download a png map of the route */
    @GET(Routes.DOWNLOAD_PNG)
    Observable<ResponseBody> downloadPng(@Path("routeId") long routeId);
    
    /** Download a png map of the route */
    @GET(Routes.DOWNLOAD_PNG)
    Observable<ResponseBody> downloadPng(@Path("routeId") String uuid);
    
    /** Download a fit version of the route */
    @GET(Routes.DOWNLOAD_FIT)
    Observable<ResponseBody> downloadFit(@Path("routeId") long routeId);
    
    /** Download a fit version of the route */
    @GET(Routes.DOWNLOAD_FIT)
    Observable<ResponseBody> downloadFit(@Path("routeId") String uuid);
    
    /** Download a gpx version of the route */
    @GET(Routes.DOWNLOAD_GPX)
    Observable<ResponseBody> downloadGpx(@Path("routeId") long routeId);
    
    /** Download a gpx version of the route */
    @GET(Routes.DOWNLOAD_GPX)
    Observable<ResponseBody> downloadGpx(@Path("routeId") String uuid);
    
    /** Update metadata about the route */
    @POST(Routes.UPDATE)
    Observable<Boolean> gpx(@Path("routeId") long routeId, @Body UserRoute route);
    
    /** Create a new route a json or fit file - the json element should be a serialized UserRoute - see utility methods below for assistance in constructing these parts */
    @Multipart
    @POST(Routes.UPLOAD)
    Observable<DataFileUploadIndex> createFromFile(@Path("format") UserRouteOutputType format, @Part MultipartBody.Part file, @Part("filename") RequestBody filename, @Part("json") RequestBody route);
    
    /** Update a route from a json or fit file - the json element should be a serialized UserRoute - see utility methods below for assistance in constructing these parts */
    @Multipart
    @POST(Routes.UPLOAD_UPDATE)
    Observable<DataFileUploadIndex> createFromFile(@Path("format") UserRouteOutputType format, @Path("routeId") long routeId, @Part MultipartBody.Part file, @Part("filename") RequestBody filename, @Part("json") RequestBody route);
    
    /** Create a new route from an existing fileId / completed activity - note that actual conversation occurs asynchronously. The route will be available once it has been processed. */
	@GET(Routes.CLONE_ACTIVITY)
    Observable<ResponseBody> createFromActivity(@Path("fileId") long fileId);
	
	/** Create a new route from an existing route - note that actual conversation occurs asynchronously. The route will be available once it has been processed. */
	@GET(Routes.CLONE_ROUTE)
    Observable<ResponseBody> createFromRoute(@Path("routeId") long routeId);
    
    public static MultipartBody.Part constructForUpload(File fit) {
    	return MultipartBody.Part.createFormData("file", fit.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), fit));
    }
    
    public static RequestBody constructForUploadFileFilename(File fit) {
    	return RequestBody.create(MediaType.parse("multipart/form-data"), fit.getName());
    }
    
    public static RequestBody constructForUpload(UserRoute meta) {
    	return RequestBody.create(MediaType.parse("multipart/form-data"), GsonManager.getInstance().toJson(meta));
    }
}
