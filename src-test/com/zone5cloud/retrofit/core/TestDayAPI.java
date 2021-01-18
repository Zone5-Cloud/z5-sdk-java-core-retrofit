package com.zone5cloud.retrofit.core;

import com.zone5cloud.core.day.UserDay;
import com.zone5cloud.core.day.UserDaySearch;
import com.zone5cloud.core.search.MappedSearchResult;
import com.zone5cloud.core.search.SearchInput;
import org.junit.Test;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;


public class TestDayAPI extends BaseTestRetrofit {

    @Test
    public void testSearchDay(){

        SearchInput<UserDaySearch> searchInput = new SearchInput<>(new UserDaySearch());
        UserDaySearch daySearch = new UserDaySearch();

        Long userId = 1L;
        List<Long> userIds = new ArrayList<Long>();
        userIds.add(userId);
        daySearch.setUserIds(userIds);
        daySearch.setYear(2021);

        Response<MappedSearchResult<UserDay>> response =  dayAPI.search(searchInput, 1,1).blockingFirst();
        assertTrue(response.isSuccessful());
        assertTrue(response.body().getCnt() > 0);
        int count = response.body().getCnt();
        if(count > 0){
           Response<MappedSearchResult<UserDay>> resultResponse = dayAPI.next(5,5).blockingFirst();
           assertTrue(resultResponse.isSuccessful());
        }
    }
}
