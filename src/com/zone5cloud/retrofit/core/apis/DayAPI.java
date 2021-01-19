package com.zone5cloud.retrofit.core.apis;

import com.zone5cloud.core.day.Day;
import com.zone5cloud.core.day.UserDay;
import com.zone5cloud.core.day.UserDaySearch;
import com.zone5cloud.core.search.MappedSearchResult;
import com.zone5cloud.core.search.SearchInput;
import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;


public interface DayAPI {

	/**
	 * Search User Day by setting the search parameters
	 * @param search json input that sets the search fields and criteria (sports type , date range)
	 * @param offset
	 * @param count
	 * @return
	 */
	@POST(Day.SEARCH)
	Observable<Response<MappedSearchResult<UserDay>>> search(@Path("offset") int offset, @Path("count") int count, @Body SearchInput<UserDaySearch> search);

	/**
	 * Allows pagination of the search results to iterate over the search results returned
	 * @param offset
	 * @param count
	 * @return
	 */
	@POST(Day.NEXT)
	Observable<Response<MappedSearchResult<UserDay>>> next(@Path("offset") int offset, @Path("count") int count);

}
