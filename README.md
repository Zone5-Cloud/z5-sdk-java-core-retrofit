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

ClientConfig config = new ClientConfig();
config.setClientID("AppKey");
config.setClientSecret("AppKeySecret");
config.setLogger(myILoggerImplementation);
config.setZone5BaseUrl(new URL(server));

// optional to restore a logged in session across App restarts, these are automatically updated on login, logout, accessToken and refresh
OAuthToken token = <restore from persistent storage>;
String username = <restore from persistent storage>;
config.setToken(token);
config.setUserName(username);
        
OkHttpClientInterceptor_Authorization auth = new OkHttpClientInterceptor_Authorization(config);
OkHttpClientInterceptor_NoDecorate nodecorate = new OkHttpClientInterceptor_NoDecorate();
		
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

## Pulling in dependency using Gradle

This library is published to GitHub Packages and can be pulled into and Android project as a Gradle dependency.

### Example
top level build.gradle file
```
allprojects {
    repositories {
        google()
        jcenter()

	maven {
            url = uri("https://maven.pkg.github.com/Zone5-Cloud/z5-sdk-java-core-retrofit")
            credentials {
                username = "<github user>"
                password = "<github access token>"
            }
        }
    ...
```
module build.gradle file
```
dependencies {

    implementation('com.zone5cloud:z5-sdk-java-core-retrofit:2.0.0')
    ...
}
```
