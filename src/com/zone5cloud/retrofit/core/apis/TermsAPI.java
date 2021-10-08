package com.zone5cloud.retrofit.core.apis;

import java.util.List;

import com.zone5cloud.core.annotations.Unauthenticated;
import com.zone5cloud.core.terms.Terms;
import com.zone5cloud.core.terms.TermsAndConditions;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

public interface TermsAPI {

	@GET(Terms.REQUIRED)
	@Unauthenticated
	Observable<Response<List<TermsAndConditions>>> required();

	@GET(Terms.DOWNLOAD)
	@Unauthenticated
	@Streaming
	Observable<Response<ResponseBody>> download(@Path("termsId") String termsId);

	@POST(Terms.ACCEPT)
	Observable<Response<Void>> accept(@Path("termsId") String termsId);

}
