package com.zone5ventures.retrofit.core.apis;

import java.io.File;

import com.zone5ventures.core.activities.Activities;
import com.zone5ventures.core.activities.DataFileUploadContext;
import com.zone5ventures.core.activities.DataFileUploadIndex;
import com.zone5ventures.core.activities.DataFileUploadRecent;
import com.zone5ventures.core.activities.UserWorkoutFileSearch;
import com.zone5ventures.core.activities.UserWorkoutResult;
import com.zone5ventures.core.enums.ActivityResultType;
import com.zone5ventures.core.enums.IntensityZoneType;
import com.zone5ventures.core.search.MappedResult;
import com.zone5ventures.core.search.MappedSearchResult;
import com.zone5ventures.core.search.SearchInput;
import com.zone5ventures.core.search.SearchInputReport;
import com.zone5ventures.core.utils.GsonManager;

import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ActivitiesAPI {
	
	/** 
	 * Initiate a search for activities - search for upcoming or completed activities.<br>
	 * Use the offset and count for a paginated result set. <br>
	 * Offset is the index in the result set to return from (starting from 0), and count is the max number of records to return from that index<br>
	 * Use the next call to retrieve the next batch of results.<br>
	 * Remember, if you have scope to multiple users to include the userIds in the criteria to restrict to specific users records.
	 */
    @POST(Activities.SEARCH)
    Observable<Response<MappedSearchResult<UserWorkoutResult>>> search(@Path("offset") int offset, @Path("count") int count, @Body SearchInput<UserWorkoutFileSearch> criteria);

    /** Get the next paginated batch - relative to the previous search query */
    @GET(Activities.NEXT)
    Observable<Response<MappedSearchResult<UserWorkoutResult>>> next(@Path("offset") int offset, @Path("count") int count);
    
    /**
     * file = MultipartBody.Part.createFormData("file", fit.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), fit));<br>
     * filename = RequestBody.create(MediaType.parse("multipart/form-data"), fit.getName())<br>
     * meta = RequestBody.create(MediaType.parse("multipart/form-data"), GsonManager.getInstance().toJson(meta));<br>
     * @param file
     * @param filename
     * @param meta (optional)
     * @return
     */
    @Multipart
    @POST(Activities.UPLOAD)
    Observable<Response<DataFileUploadIndex>> upload(@Part MultipartBody.Part file, @Part("filename") RequestBody filename, @Part("json") RequestBody meta);
    
    /** Use the DataFileUploadIndex.id from the upload call to request the file processing status */
    @GET(Activities.FILE_INDEX_STATUS)
    Observable<Response<DataFileUploadIndex>> uploadStatus(@Path("indexId") long indexId);
    
    /** Use the DataFileUploadIndex.id to cancel a file upload / delete file which has failed to process */
    @GET(Activities.FILE_INDEX_CANCEL)
    Observable<Response<Boolean>> cancelUpload(@Path("indexId") long indexId);
    
    /** Use the DataFileUploadIndex.id to request that a failed file be re-processed / re-attempted. If the file has been successfully processed before, this endpoint will re-queue the file for re-processing. */
    @GET(Activities.FILE_INDEX_RETRY)
    Observable<Response<DataFileUploadIndex>> retryUpload(@Path("indexId") long indexId);
    
    /** Use the DataFileUploadIndex.id to download the original uploaded file - this can be used even if the file could not be processed */
    @GET(Activities.FILE_INDEX_DOWNLOAD)
    Observable<Response<ResponseBody>> downloadUpload(@Path("indexId") long indexId);
 
    /** Query for currently processing files, and files which have been uploaded or reprocessed within the last X seconds */
    @GET(Activities.FILE_INDEX_STATUS_RECENT)
    Observable<Response<DataFileUploadRecent>> uploadStatus(@Path("userId") long userId, @Path("secs") int secs);
    
    /** Delete a file, workout or event */
    @GET(Activities.DELETE)
    Observable<Response<Boolean>> delete(@Path("activityType") ActivityResultType activityType, @Path("activityId") long activityId);
    
    /** Download a fit file - the fileId is the same as the activityId, when the activityType=files. It's also provided in any activities search result */
    @GET(Activities.DOWNLOAD_FIT)
    Observable<Response<ResponseBody>> downloadFit(@Path("fileId") long fileId);
    
    /** Download a fit file which has normalized channel data - useful for timeseries graphs */
    @GET(Activities.DOWNLOAD_RAW3)
    Observable<Response<ResponseBody>> downloadRaw(@Path("fileId") long fileId);
    
    /** Download a csv file which has normalized channel data */
    @GET(Activities.DOWNLOAD_CSV)
    Observable<Response<ResponseBody>> downloadCsv(@Path("fileId") long fileId);
    
    /** Download a png file which is a static map of the completed route */
    @GET(Activities.DOWNLOAD_MAP)
    Observable<Response<ResponseBody>> downloadMap(@Path("fileId") long fileId);
    
    /** Download a png file which is a static map of the completed route */
    @POST(Activities.TIME_IN_ZONE)
    Observable<Response<MappedResult<UserWorkoutResult>>> timeInZones(@Path("zoneType") IntensityZoneType zoneType, @Body SearchInputReport criteria);
    
    @POST(Activities.PEAK_POWER)
    Observable<Response<MappedResult<UserWorkoutResult>>> peakPowerCurve(@Body SearchInputReport criteria);
   
    @POST(Activities.PEAK_HEARTRATE)
    Observable<Response<MappedResult<UserWorkoutResult>>> peakHeartRateCurve(@Body SearchInputReport criteria);
    
    @POST(Activities.PEAK_WKG)
    Observable<Response<MappedResult<UserWorkoutResult>>> peakWattsKgCurve(@Body SearchInputReport criteria);
    
    @POST(Activities.PEAK_PACE)
    Observable<Response<MappedResult<UserWorkoutResult>>> peakPaceCurve(@Body SearchInputReport criteria);
    
    @POST(Activities.PEAK_LSS)
    Observable<Response<MappedResult<UserWorkoutResult>>> peakLegSpringStiffnessCurve(@Body SearchInputReport criteria);
    
    @POST(Activities.PEAK_LSSKG)
    Observable<Response<MappedResult<UserWorkoutResult>>> peakLegSpringStiffnessKgCurve(@Body SearchInputReport criteria);
    
    
    /** 
     * Associate a bikeId (Specialized bikeId) with a completed activity - note that this call may be processed asynchronously.
     * 
     * If the given bikeId has not been previously associated with a ride of this user, then the S-Digital query to lookup the bikeId will be performed in the future, and on that external lookup the association in the TP/Z5 system will be made.
     *  
     * This method will return true if the bikeId was accepted for being changed. 
     */
    @GET(Activities.SET_BIKE)
    Observable<Response<Boolean>> setBikeId(
    		@Path("activityType") ActivityResultType activityType, 
    		@Path("activityId") long activityId,
    		@Path("bikeId") String bikeId);
    
    /** Remove any bikeId which is associated with a completed activity - returns true on success */
    @GET(Activities.REM_BIKE)
    Observable<Response<Boolean>> removeBikeId(
    		@Path("activityType") ActivityResultType activityType, 
    		@Path("activityId") long activityId);
    
    
    public static MultipartBody.Part constructForFileUpload(File fit) {
    	return MultipartBody.Part.createFormData("file", fit.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), fit));
    }
    
    public static RequestBody constructForFileUploadFileFilename(File fit) {
    	return RequestBody.create(MediaType.parse("multipart/form-data"), fit.getName());
    }
    
    public static RequestBody constructForFileUpload(DataFileUploadContext meta) {
    	return RequestBody.create(MediaType.parse("multipart/form-data"), GsonManager.getInstance().toJson(meta));
    }
}
