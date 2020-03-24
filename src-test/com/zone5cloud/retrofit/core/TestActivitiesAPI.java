package com.zone5cloud.retrofit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;

import com.zone5cloud.retrofit.core.apis.ActivitiesAPI;
import com.zone5cloud.core.activities.Activities;
import com.zone5cloud.core.activities.DataFileUploadContext;
import com.zone5cloud.core.activities.DataFileUploadIndex;
import com.zone5cloud.core.activities.UserWorkoutFileSearch;
import com.zone5cloud.core.activities.UserWorkoutResult;
import com.zone5cloud.core.activities.VActivity;
import com.zone5cloud.core.enums.ActivityResultType;
import com.zone5cloud.core.enums.ActivityType;
import com.zone5cloud.core.enums.Equipment;
import com.zone5cloud.core.enums.FileUploadState;
import com.zone5cloud.core.enums.IntensityZoneType;
import com.zone5cloud.core.enums.RelativePeriod;
import com.zone5cloud.core.enums.UserWorkoutState;
import com.zone5cloud.core.enums.WorkoutType;
import com.zone5cloud.core.search.DateRange;
import com.zone5cloud.core.search.MappedResult;
import com.zone5cloud.core.search.MappedSearchResult;
import com.zone5cloud.core.search.Order;
import com.zone5cloud.core.search.SearchInput;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class TestActivitiesAPI extends BaseTestRetrofit {
	
	@Test
	public void testUploadWithNoMetadata() throws Exception {
		// a completed ride file
		File fit = new File("../z5-sdk-java-core/test-resources/2013-12-22-10-30-12.fit");
		assertTrue(fit.exists());
		
		MultipartBody.Part filePart = ActivitiesAPI.constructForFileUpload(fit);
		RequestBody filenamePart    = ActivitiesAPI.constructForFileUploadFileFilename(fit);
		RequestBody metaPart        = null;
		
		DataFileUploadIndex index = activitiesApi.upload(filePart, filenamePart, metaPart).blockingFirst().body();
		
		assertNotNull(index.getId()); // file processing index id
		if (index.getState() == FileUploadState.finished) {
			assertTrue(activitiesApi.delete(ActivityResultType.files, index.getResultId()).blockingFirst().body());
			return;
		}
		
		assertTrue(index.getState() == FileUploadState.pending || index.getState() == FileUploadState.queued);
		
		// Wait for it to process
		while(index.getState() != FileUploadState.finished) {
			Thread.sleep(1000L);
			index = activitiesApi.uploadStatus(index.getId()).blockingFirst().body();
		}
		
		assertNotNull(index.getResultId());
		
		// Search specifically for this file by it's resultId
		SearchInput<UserWorkoutFileSearch> search = new SearchInput<>(new UserWorkoutFileSearch());
		search.getCriteria().setActivities(Arrays.asList(new VActivity(index.getResultId(), ActivityResultType.files)));
		search.setFields(Arrays.asList("name", "distance", "ascent", "peak3minWatts", "peak20minWatts", "channels"));
		MappedSearchResult<UserWorkoutResult> results = activitiesApi.search(0, 1, search).blockingFirst().body();
		assertEquals(1, results.getResult().getResults().size());
		
		// DataFileUploadIndex.resultId === fileId
		assertEquals(index.getResultId(), results.getResult().getResults().get(0).getFileId());
		assertEquals(index.getResultId(), results.getResult().getResults().get(0).getActivityId());
		assertEquals(ActivityResultType.files, results.getResult().getResults().get(0).getActivity());
		
		File f = toFile(activitiesApi.downloadFit(results.getResult().getResults().get(0).getFileId()).blockingFirst().body());
		assertTrue(f.exists() && f.length() == fit.length());
		
		f = toFile(activitiesApi.downloadMap(results.getResult().getResults().get(0).getFileId()).blockingFirst().body());
		assertTrue(f.exists() && f.length() > 0);	
		
		f = toFile(activitiesApi.downloadRaw(results.getResult().getResults().get(0).getFileId()).blockingFirst().body());
		assertTrue(f.exists() && f.length() > 0);
		
		f = toFile(activitiesApi.downloadCsv(results.getResult().getResults().get(0).getFileId()).blockingFirst().body());
		assertTrue(f.exists() && f.length() > 0);
		
		// Search by filename
		search = new SearchInput<>(new UserWorkoutFileSearch());
		search.getCriteria().setName(fit.getName());
		search.setFields(Arrays.asList("name", "distance", "ascent", "peak3minWatts", "peak20minWatts", "channels"));
		results = activitiesApi.search(0, 1, search).blockingFirst().body();
		assertEquals(1, results.getResult().getResults().size());
		assertEquals(index.getResultId(), results.getResult().getResults().get(0).getFileId());
		
		// Get the power time in zones breakdown
		MappedResult<UserWorkoutResult> zones = activitiesApi.timeInZones(IntensityZoneType.pwr, Activities.newInstance(ActivityResultType.files, results.getResult().getResults().get(0).getFileId())).blockingFirst().body();
		assertEquals(1, zones.getResults().size());
		assertTrue(zones.getResults().get(0).getPwrZones().size() > 0);
		assertNotNull(zones.getResults().get(0).getThresholdWatts());
		
		// Get the power curve for this activity - with the best ever relative curve
		MappedResult<UserWorkoutResult> powercurve = activitiesApi.peakPowerCurve(Activities.newInstancePeaksCurve(ActivityResultType.files, results.getResult().getResults().get(0).getFileId(), RelativePeriod.alltime)).blockingFirst().body();
		assertNotNull(powercurve.getResults().get(0).getPeak3secWatts());
		
		// Delete it
		assertTrue(activitiesApi.delete(ActivityResultType.files, index.getResultId()).blockingFirst().body());
	}
	
	@Test
	public void testUploadWithMetadata() throws Exception {
		// a completed ride file
		File fit = new File("../z5-sdk-java-core/test-resources/2013-12-22-10-30-12.fit");
		assertTrue(fit.exists());
		
		// Set an alternate name and equipment type
		DataFileUploadContext c = new DataFileUploadContext();
		c.setEquipment(Equipment.gravel);
		c.setName("Epic ride");

		MultipartBody.Part filePart = ActivitiesAPI.constructForFileUpload(fit);
		RequestBody filenamePart    = ActivitiesAPI.constructForFileUploadFileFilename(fit);
		RequestBody metaPart        = ActivitiesAPI.constructForFileUpload(c);
		
		DataFileUploadIndex index = activitiesApi.upload(filePart, filenamePart, metaPart).blockingFirst().body();
		
		assertNotNull(index.getId()); // file processing index id
		if (index.getState() == FileUploadState.finished) {
			assertTrue(activitiesApi.delete(ActivityResultType.files, index.getResultId()).blockingFirst().body());
			return;
		}
		
		assertTrue(index.getState() == FileUploadState.pending || index.getState() == FileUploadState.queued);
		
		// Wait for it to process
		while(index.getState() != FileUploadState.finished) {
			Thread.sleep(1000L);
			index = activitiesApi.uploadStatus(index.getId()).blockingFirst().body();
		}
		
		// Search specifically for this file by it's resultId - and make sure our custom name and equipment type stuck
		SearchInput<UserWorkoutFileSearch> search = new SearchInput<>(new UserWorkoutFileSearch());
		search.getCriteria().setActivities(Arrays.asList(new VActivity(index.getResultId(), ActivityResultType.files)));
		search.setFields(Arrays.asList("name", "equipment"));
		MappedSearchResult<UserWorkoutResult> results = activitiesApi.search(0, 1, search).blockingFirst().body();
		assertEquals(1, results.getResult().getResults().size());
		assertEquals("Epic ride", results.getResult().getResults().get(0).getName());
		assertEquals(Equipment.gravel, results.getResult().getResults().get(0).getEquipment());

		// Delete it
		assertTrue(activitiesApi.delete(ActivityResultType.files, index.getResultId()).blockingFirst().body());
	}

	@Test
	public void testSearchForLast10Activities() throws Exception {
		SearchInput<UserWorkoutFileSearch> search = new SearchInput<>(new UserWorkoutFileSearch());
		
		// Just request some summary fields
		search.setFields(Arrays.asList("name", "distance", "training", "avgWatts", "avgBpm", "lat1", "lon1", "startTs", "locality", "peak3secWatts", "headunit.name", "aap.avgWatts"));
		
		// We only want completed rides with files
		search.getCriteria().setIsNotNull(Arrays.asList("fileId"));
		
		// Order by ride start time desc
		search.getCriteria().setOrder(Arrays.asList(new Order("startTs", com.zone5cloud.core.enums.Order.desc)));
		
		// Limit to rides with a startTs <= now - ie avoid dodgy files which might have a timestamp in the future!
		search.getCriteria().setToTs(System.currentTimeMillis());
			
		MappedSearchResult<UserWorkoutResult> result = activitiesApi.search(0, 10, search).blockingFirst().body();
		
		// The total number of results which are available (assumes you have uploaded at least 1 ride! 
        assertTrue(result.getCnt() > 0);
        
        // Confirm we just have the sub-set we requested
		assertTrue(result.getResult().getResults().size() > 0 && result.getResult().getResults().size()<=10);
				
		for(UserWorkoutResult r : result.getResult().getResults()) {
			assertNotNull(r.getName());
			assertNotNull(r.getFileId());
		}
	}
	
	@Test
	public void testSearchForRidesOfSpecificDistance() throws Exception {
		SearchInput<UserWorkoutFileSearch> search = new SearchInput<>(new UserWorkoutFileSearch());
		
		// Just request some summary fields - including power meter manufacturer, battery level and eBike power info
		search.setFields(Arrays.asList("name", "distance", "training", "pwrManufacturer", "pwrBattery", "turbo.avgMotorPower"));
		
		// We only want completed rides with files
		search.getCriteria().setIsNotNull(Arrays.asList("fileId"));
		search.getCriteria().setRanges(new HashMap<String, List<Double>>());
		search.getCriteria().getRanges().put("distance", Arrays.asList(20000d, 30000d)); // 20-30km
		
		// We only want rides
		search.getCriteria().setSports(Arrays.asList(ActivityType.ride));
		
		// Order by ride start time desc
		search.getCriteria().setOrder(Arrays.asList(new Order("startTs", com.zone5cloud.core.enums.Order.desc)));
		
		// Limit to rides which were done in the last 12 months
		search.getCriteria().setFromTs(System.currentTimeMillis() - (1000L*60*60*24*365));
		
		MappedSearchResult<UserWorkoutResult> results = activitiesApi.search(0, 10, search).blockingFirst().body();
		
		assertTrue(results.getResult().getResults().size() <= 10);
		for(UserWorkoutResult r : results.getResult().getResults())
			assertTrue(r.getDistance() >= 20000 && r.getDistance() <= 30000);
		
		if (results.getCnt() > 10) {
			// Get the next batch of 10
			results = activitiesApi.next(10, 10).blockingFirst().body();
			assertTrue(results.getResult().getResults().size() <= 10);
			for(UserWorkoutResult r : results.getResult().getResults())
				assertTrue(r.getDistance() >= 20000 && r.getDistance() <= 30000);
		}	
	}
	
	@Test
	public void testSearchForRidesInSpecificDateRanges() throws Exception {
		SearchInput<UserWorkoutFileSearch> search = new SearchInput<>(new UserWorkoutFileSearch());
		
		// Just request some summary fields - including power meter manufacturer, battery level and eBike power info
		search.setFields(Arrays.asList("name", "distance", "training"));
		
		// We only want completed rides with files
		search.getCriteria().setIsNotNull(Arrays.asList("fileId"));
		
		search.getCriteria().setRangesTs(new ArrayList<>());
		
		Calendar c = new GregorianCalendar(TimeZone.getTimeZone("Australia/Sydney"));
		c.set(Calendar.YEAR, 2018);
		c.set(Calendar.MONTH, Calendar.JANUARY);
		c.set(Calendar.DAY_OF_YEAR, 1);
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		
		// First 30 days of 2018
		search.getCriteria().getRangesTs().add(new DateRange(c.getTimeInMillis(), c.getTimeInMillis()+(1000L*60*60*24*30), "Australia/Sydney"));
		
		c.add(Calendar.YEAR, 1);
		// First 30 days of 2019
		search.getCriteria().getRangesTs().add(new DateRange(c.getTimeInMillis(), c.getTimeInMillis()+(1000L*60*60*24*30), "Australia/Sydney"));
		
		MappedSearchResult<UserWorkoutResult> results = activitiesApi.search(0, 10, search).blockingFirst().body();
		assertNotNull(results);
	}
	
	@Test
	public void testSearchForIncompleteWorkouts() throws Exception {
		
		SearchInput<UserWorkoutFileSearch> search = new SearchInput<>(new UserWorkoutFileSearch());
		
		search.setFields(Arrays.asList("scheduled.name", "scheduled.day", "scheduled.tz", "workout"));
		
		search.getCriteria().setToTs(System.currentTimeMillis());
		
		search.getCriteria().setIsNull(Arrays.asList("fileId"));
		search.getCriteria().setState(UserWorkoutState.pending);
		search.getCriteria().setExcludeWorkouts(Arrays.asList(WorkoutType.rest));
		
		search.getCriteria().setOrder(Arrays.asList(new Order("scheduled.day", com.zone5cloud.core.enums.Order.desc)));
		
		MappedSearchResult<UserWorkoutResult> results = activitiesApi.search(0, 10, search).blockingFirst().body();
		assertNotNull(results);
	}
	
	@Test
	public void testSearchForUpcomingWorkouts() throws Exception {
		SearchInput<UserWorkoutFileSearch> search = new SearchInput<>(new UserWorkoutFileSearch());
		
		search.setFields(Arrays.asList("scheduled.day", "scheduled.tz", "scheduled.name", "scheduled.tscorepwr", "scheduled.durationSecs", "scheduled.distance", "scheduled.workout", "scheduled.preDescr"));
		search.getCriteria().setOrder(Arrays.asList(new Order("scheduled.day", com.zone5cloud.core.enums.Order.asc)));
		search.getCriteria().setFromTs(System.currentTimeMillis());
		
		MappedSearchResult<UserWorkoutResult> results = activitiesApi.search(0, 10, search).blockingFirst().body();
		assertNotNull(results);
	}
	
	@Test
	public void testComplexSearch() throws Exception {
		SearchInput<UserWorkoutFileSearch> search = new SearchInput<>(new UserWorkoutFileSearch());
		
		search.setFields(Arrays.asList("name", "distance", "ascent", "peak3minWatts", "peak20minWatts"));
		search.setCriteria(new UserWorkoutFileSearch());
		search.getCriteria().setRanges(new HashMap<>());
		
		// Distance 20-50km
		search.getCriteria().getRanges().put("distance", Arrays.asList(20000d,50000d));
		
		// Ascent 10-1000m
		search.getCriteria().getRanges().put("ascent", Arrays.asList(10d,1000d));
		
		// At last 30mins in duration
		search.getCriteria().getRanges().put("training", Arrays.asList(30*60d));
		
		// Has cadence and heart rate
		search.getCriteria().setIsNotNull(Arrays.asList("avgCadence", "avgBpm"));
		
		// Only road rides
		search.getCriteria().setEquipment(Equipment.road);
		
		// Only rides
		search.getCriteria().setSports(Arrays.asList(ActivityType.ride));
		
		// Contains this name
		search.getCriteria().setName("Foo");
		
		
		MappedSearchResult<UserWorkoutResult> results = activitiesApi.search(0, 10, search).blockingFirst().body();
		assertNotNull(results);
	}
	
	private File toFile(ResponseBody b) throws IOException {
		File f = File.createTempFile(getClass().getSimpleName(), "tmp");
		f.deleteOnExit();
		
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(f);
			out.write(b.bytes());
			out.flush();
			
		} finally {
			if (out != null)
				try { out.close(); } catch (IOException e) { }
		}
		System.out.println(f.getAbsolutePath());
		return f;
	}
	
}
