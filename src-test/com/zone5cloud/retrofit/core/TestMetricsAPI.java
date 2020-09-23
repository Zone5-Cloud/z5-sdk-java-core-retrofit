package com.zone5cloud.retrofit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Test;

import com.zone5cloud.core.activities.Activities;
import com.zone5cloud.core.activities.UserWorkoutResultAggregates;
import com.zone5cloud.core.search.MappedResult;
import com.zone5cloud.core.utils.GsonManager;

public class TestMetricsAPI extends BaseTestRetrofit {
	
	@Test
	public void testQuery() throws Exception {		
		MappedResult<UserWorkoutResultAggregates> result = metricsApi.metrics(Activities.newInstanceMetricsBikes(null, Arrays.asList("sum.training","sum.distance","sum.ascent","wavg.avgSpeed","max.maxSpeed","wavg.avgWatts","max.maxWatts"), Arrays.asList("d584c5cb-e81f-4fbe-bc0d-667e9bcd2c4c"))).blockingFirst().body();
		assertNotNull(result.getResults());
		assertEquals(1, result.getResults().size());
		assertNotNull(result.getResults().get(0).getSum());
		assertNotNull(result.getResults().get(0).getSum().getTraining());
		assertNotNull(result.getResults().get(0).getSum().getDistance());
		assertNotNull(result.getResults().get(0).getSum().getAscent());
		assertNotNull(result.getResults().get(0).getMax().getMaxSpeed());
		assertNotNull(result.getResults().get(0).getMax().getMaxWatts());
		
		assertNotNull(result.getResults().get(0).getWavg().getAvgSpeed());
		assertNotNull(result.getResults().get(0).getWavg().getAvgWatts());
		assertNotNull(result.getResults().get(0).getCount());
		assertNotNull(result.getResults().get(0).getUser());
		assertNotNull(result.getResults().get(0).getName());
		assertNotNull(result.getResults().get(0).getBike().getBikeUuid());
		assertNotNull(result.getResults().get(0).getBike().getRegistrationId());
		// Assuming you have a match for this bike id, you would have result which looks like;
		/* {
  "results": [
    {
      "sum": {
        "training": 3148,
        "distance": 25222.949219,
        "ascent": 132
      },
      "max": {
        "maxSpeed": 54.327599,
        "maxWatts": 689
      },
      "wavg": {
        "avgSpeed": 28.8432,
        "avgWatts": 146
      },
      "count": 1,
      "user": {
        "id": 199,
        "firstname": "Andrew",
        "lastname": "Hall"
      },
      "name": "Series",
      "bike": {
        "uuid": "d584c5cb-e81f-4fbe-bc0d-667e9bcd2c4c"
      }
    }
  ]
} */
		System.out.println(GsonManager.getInstance(true).toJson(result));
	} 
	
}
