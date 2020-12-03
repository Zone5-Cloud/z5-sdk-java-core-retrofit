package com.zone5cloud.retrofit.core.utilities;

import java.io.IOException;

import com.google.gson.JsonSyntaxException;
import com.zone5cloud.core.Types;
import com.zone5cloud.core.Z5Error;
import com.zone5cloud.core.utils.GsonManager;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class Z5Utilities {

	/** Pass in an error response to parse it into a Z5Error object */
	public static Z5Error parseErrorResponse(Response<?> response) {
		try {
			if (!response.isSuccessful()) {
				ResponseBody body = response.errorBody();
				String bodyString = body.string();
				return GsonManager.getInstance().fromJson(bodyString, Types.ERROR);
			}
		} catch (JsonSyntaxException|IOException e) {
			
		}
		
		return null;
	}
	
	private Z5Utilities() {}
}
