package com.zone5cloud.retrofit.core.apis;

import com.zone5cloud.core.day.Day;
import com.zone5cloud.core.day.UserDay;
import com.zone5cloud.core.day.UserDaySearch;
import com.zone5cloud.core.search.MappedSearchResult;
import com.zone5cloud.core.search.SearchInput;
import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.POST;
import retrofit2.http.Path;


public interface DayAPI {

	@POST(Day.SEARCH)
	Observable<Response<MappedSearchResult<UserDay>>> search(SearchInput<UserDaySearch> search, @Path("offset") int offset, @Path("count") int count);

	@POST(Day.NEXT)
	Observable<Response<MappedSearchResult<UserDay>>> next(@Path("offset") int offset, @Path("count") int count);

}
