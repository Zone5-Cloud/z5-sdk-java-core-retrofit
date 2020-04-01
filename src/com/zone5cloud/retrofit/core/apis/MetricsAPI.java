package com.zone5cloud.retrofit.core.apis;

import com.zone5cloud.core.activities.Activities;
import com.zone5cloud.core.activities.UserWorkoutResultAggregates;
import com.zone5cloud.core.search.MappedResult;
import com.zone5cloud.core.search.SearchInput;
import com.zone5cloud.core.search.SearchInputReport;

import retrofit2.http.Body;
import retrofit2.http.POST;
import io.reactivex.Observable;
import retrofit2.Response;

public interface MetricsAPI {
	
	/** 
	 * Get aggregate metrics for a given set of users or bikes over 1 or more date ranges.<br>
	 *
	 * Supported aggregates include;
	 * 
	 * <ol>
	 * <li>avg - simple average
	 * <li>min - minimum value
	 * <li>max - maximim value
	 * <li>wavg - weighted average (weighted by time)
	 * <li>sum - sum of values
	 * </ol>
	 * 
	 * See Activities.newInstanceMetrics() and Activities.newInstanceMetricsBikes() for constructing the input object 
	 * 
	 */
    @POST(Activities.METRICS)
    Observable<Response<MappedResult<UserWorkoutResultAggregates>>> metrics(@Body SearchInput<SearchInputReport> input);
}
