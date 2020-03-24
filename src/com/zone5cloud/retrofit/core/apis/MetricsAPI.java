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
	 * Get aggregate metrics for a given set of users and date ranges.<br>
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
	 * See Activities.newInstanceMetrics(sport, userIds, ranges, fields) for constructing the input object 
	 * 
	 * @param sport - required - the sport type
	 * @param userIds - required - 1 or more userIds can be requested
	 * @param ranges - the date ranges - 1 or more ranges can be requested. If the ranges overlap it is indeterministic which range the metrics will be included in.
	 * @param fields - the aggregate fields being requested
	 * 
	 */
    @POST(Activities.METRICS)
    Observable<Response<MappedResult<UserWorkoutResultAggregates>>> metrics(@Body SearchInput<SearchInputReport> input);
}
