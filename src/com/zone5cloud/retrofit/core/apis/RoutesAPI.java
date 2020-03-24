package com.zone5cloud.retrofit.core.apis;

import java.io.File;

import com.zone5cloud.core.activities.DataFileUploadIndex;
import com.zone5cloud.core.routes.Routes;
import com.zone5cloud.core.routes.UserRoute;
import com.zone5cloud.core.routes.UserRouteOutputType;
import com.zone5cloud.core.routes.UserRouteSearch;
import com.zone5cloud.core.search.SearchResult;
import com.zone5cloud.core.utils.GsonManager;

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
import io.reactivex.Observable;
import retrofit2.Response;


public interface RoutesAPI {
	
	/** Search for routes */
	@POST(Routes.SEARCH)
    Observable<Response<SearchResult<UserRoute>>> search(@Path("offset") int offset, @Path("count") int count, @Body UserRouteSearch criteria);	
	
    /** Get the next paginated batch - relative to the previous search query */
    @GET(Routes.NEXT)
    Observable<Response<SearchResult<UserRoute>>> next(@Path("offset") int offset, @Path("count") int count);
    
    /** Get summary information about the route */
    @GET(Routes.SUMMARY)
    Observable<Response<UserRoute>> summary(@Path("routeId") long routeId);
    
    /** Get summary information about the route */
    @GET(Routes.SUMMARY)
    Observable<Response<UserRoute>> summary(@Path("routeId") String uuid);
    
    /** Get detailed information about the route */
    @GET(Routes.DETAILED)
    Observable<Response<UserRoute>> detailed(@Path("routeId") long routeId);
    
    /** Get detailed information about the route */
    @GET(Routes.DETAILED)
    Observable<Response<UserRoute>> detailed(@Path("routeId") String uuid);
    
    /** Delete a route */
    @GET(Routes.DELETE)
    Observable<Response<Boolean>> delete(@Path("routeId") long routeId);
    
    /** Download a png map of the route */
    @GET(Routes.DOWNLOAD_PNG)
    Observable<Response<ResponseBody>> downloadPng(@Path("routeId") long routeId);
    
    /** Download a png map of the route */
    @GET(Routes.DOWNLOAD_PNG)
    Observable<Response<ResponseBody>> downloadPng(@Path("routeId") String uuid);
    
    /** Download a fit version of the route */
    @GET(Routes.DOWNLOAD_FIT)
    Observable<Response<ResponseBody>> downloadFit(@Path("routeId") long routeId);
    
    /** Download a fit version of the route */
    @GET(Routes.DOWNLOAD_FIT)
    Observable<Response<ResponseBody>> downloadFit(@Path("routeId") String uuid);
    
    /** Download a gpx version of the route */
    @GET(Routes.DOWNLOAD_GPX)
    Observable<Response<ResponseBody>> downloadGpx(@Path("routeId") long routeId);
    
    /** Download a gpx version of the route */
    @GET(Routes.DOWNLOAD_GPX)
    Observable<Response<ResponseBody>> downloadGpx(@Path("routeId") String uuid);
    
    /** Update metadata about the route */
    @POST(Routes.UPDATE)
    Observable<Response<Boolean>> gpx(@Path("routeId") long routeId, @Body UserRoute route);
    
    /** Create a new route a json or fit file - the json element should be a serialized UserRoute - see utility methods below for assistance in constructing these parts */
    @Multipart
    @POST(Routes.UPLOAD)
    Observable<Response<DataFileUploadIndex>> createFromFile(@Path("format") UserRouteOutputType format, @Part MultipartBody.Part file, @Part("filename") RequestBody filename, @Part("json") RequestBody route);
    
    /** Update a route from a json or fit file - the json element should be a serialized UserRoute - see utility methods below for assistance in constructing these parts */
    @Multipart
    @POST(Routes.UPLOAD_UPDATE)
    Observable<Response<DataFileUploadIndex>> createFromFile(@Path("format") UserRouteOutputType format, @Path("routeId") long routeId, @Part MultipartBody.Part file, @Part("filename") RequestBody filename, @Part("json") RequestBody route);
    
    /** Create a new route from an existing fileId / completed activity - note that actual conversation occurs asynchronously. The route will be available once it has been processed. */
	@GET(Routes.CLONE_ACTIVITY)
    Observable<Response<ResponseBody>> createFromActivity(@Path("fileId") long fileId);
	
	/** Create a new route from an existing route - note that actual conversation occurs asynchronously. The route will be available once it has been processed. */
	@GET(Routes.CLONE_ROUTE)
    Observable<Response<ResponseBody>> createFromRoute(@Path("routeId") long routeId);
    
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
