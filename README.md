# z5-sdk-java-core-retrofit

This project provides API interfaces for use with the Retrofit2 and OkHttpClient clients.

In particular, the following elements are provided;

* OkHttpClientInterceptor_NoDecorate.java - An okhttp3.Interceptor which sets a custom HTTP header
* OkHttpClientInterceptor_Authorization.java - An okhttp3.Interceptor which sets the user's authorization bearer token header
* OkHttpClientCookieJar.java - An okhttp3.CookieJar for accepting and setting JSESSIONID and AWS LB set-cookie headers

* UserAPI.java - An interface decorated for use with Retrofit (basic user operations)
* ActivitiesAPI.java - An interface decorated for use with Retrofit (activity operations - file uploads, file search, file metrics etc)

* GsonManager.java - Can provice you with a Gson instance for serialization/deserialization. If you already have a Gson instance, use the GsonManager.decorate(builder) to decorate your Gson instance with the Z5 type adapters.

```
String server = "https://staging.todaysplan.com.au";
String authToken = "my user bearer token"
		
OkHttpClientInterceptor_NoDecorate nodecorate = new OkHttpClientInterceptor_NoDecorate();
OkHttpClientInterceptor_Authorization auth = new OkHttpClientInterceptor_Authorization(authToken);
		
Gson gson = GsonManager.getInstance();
# Alternatively, if you have your own GsonBuilder - use GsonManager.decorate(builder)

Retrofit retrofit = new Retrofit.Builder()
  .baseUrl(server)
  .client(
     new OkHttpClient()
	.newBuilder().cookieJar(new OkHttpClientCookieJar())
	.addInterceptor(nodecorate)
 	.addInterceptor(auth)
	.build()
     )
  .addConverterFactory(GsonConverterFactory.create(gson))
  .build();

UserAPI userApi = retrofit.create(UserAPI.class);
ActivitiesAPI activitiesApi = retrofit.create(ActivitiesAPI.class);
```

Example usage of the apis can be found in TestActivitiesAPI.java and TestUsersAPI.java. 
