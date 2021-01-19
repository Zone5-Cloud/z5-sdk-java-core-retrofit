package com.zone5cloud.retrofit.core;

import com.zone5cloud.core.day.UserDay;
import com.zone5cloud.core.day.UserDaySearch;
import com.zone5cloud.core.enums.ActivityType;
import com.zone5cloud.core.search.DateRange;
import com.zone5cloud.core.search.DayRange;
import com.zone5cloud.core.search.MappedSearchResult;
import com.zone5cloud.core.search.SearchInput;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;


public class TestDayAPI extends BaseTestRetrofit {

    @Before
    public void setup() {
        login();
    }

    @Test
    public void testSearchDay(){

        SearchInput<UserDaySearch> search = new SearchInput<>(new UserDaySearch());
        UserDaySearch daySearch = new UserDaySearch();

        search.setFields(Arrays.asList("activity.training", "activity.distance", "att.weight", "steps",
                "sleep.sleepMinsAsleep", "rating.fatigue", "load.ride.tscore.ctl", "load.ride.tscore.tsb",
                "schedule.id"));

        List<ActivityType> sports = new ArrayList<>();
        sports.add(ActivityType.ride);
        daySearch.setSports(sports);
        List<DateRange> ranges = new ArrayList<>();

        DateRange dateRange = new DateRange();
        dateRange.setCeilTs(1574686800000L);
        dateRange.setFloorTs(1577710800000L);
        ranges.add(dateRange);

        daySearch.setRanges(ranges);
        search.setCriteria(daySearch);

        Response<MappedSearchResult<UserDay>> response =  dayAPI.search(1,1, search).blockingFirst();
        assertTrue(response.isSuccessful());
        assertTrue(response.body().getCnt() > 0);
        int count = response.body().getCnt();
        System.out.println(" Count of responses "+response.body().getCnt());
        if(count > 0){
           Response<MappedSearchResult<UserDay>> resultResponse = dayAPI.next(5,5).blockingFirst();
           assertTrue(resultResponse.isSuccessful());
        }
    }
}
