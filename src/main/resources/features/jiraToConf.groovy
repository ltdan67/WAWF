package features

import com.atlassian.applinks.api.ApplicationLink
import com.atlassian.applinks.api.ApplicationLinkService
import com.atlassian.applinks.api.application.confluence.ConfluenceApplicationType
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.sal.api.net.Request
import com.atlassian.sal.api.net.Response
import com.atlassian.sal.api.net.ResponseException
import com.atlassian.sal.api.net.ResponseHandler
import com.atlassian.sal.api.net.ReturningResponseHandler
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper


def ApplicationLink getPrimaryConfluenceLink() {
    def applicationLinkService = ComponentLocator.getComponent(ApplicationLinkService.class)
    final ApplicationLink conflLink = applicationLinkService.getPrimaryApplicationLink(ConfluenceApplicationType.class);
    conflLink
}

def confluenceLink = getPrimaryConfluenceLink()
assert confluenceLink // must have a working app link set up
def authenticatedRequestFactory = confluenceLink.createAuthenticatedRequestFactory()


def createSpace = { spaceKey, spaceName, spaceDescription ->
    def paramsSpace = [
            key: spaceKey,
            name: spaceName,
            type: "global",
            description: [
                    plain: [
                            value: spaceDescription,
                            representation: "plain"
                    ]
            ]
    ]

    // Create a confluence space
    authenticatedRequestFactory
            .createRequest(Request.MethodType.POST, "/rest/api/space")
            .addHeader("Content-Type", "application/json")
            .addHeader('Authorization', 'Basic ' + 'admin:admin'.bytes.encodeBase64().toString())
            .setRequestBody(new JsonBuilder(paramsSpace).toString())
            .execute(new ResponseHandler<Response>() {
        @Override
        void handle(Response response) throws ResponseException {
            if (response.statusCode != HttpURLConnection.HTTP_OK) {
                throw new Exception(response.getResponseBodyAsString())
            }
        }
    })

}

def createPage = { spaceKey, pageTitle, pageContent ->
    def paramsPage = [
            type : "page",
            title: pageTitle,
            space: [
                    key: spaceKey
            ],
            body : [
                    storage: [
                            value: pageContent,
                            representation: "storage"

                    ]
            ]
    ]

    // Create a confluence page
    authenticatedRequestFactory
            .createRequest(Request.MethodType.POST, "/rest/api/content")
            .addHeader("Content-Type", "application/json")
            .addHeader('Authorization', 'Basic ' + 'admin:admin'.bytes.encodeBase64().toString())
            .setRequestBody(new JsonBuilder(paramsPage).toString())
            .execute(new ResponseHandler<Response>() {
        @Override
        void handle(Response response) throws ResponseException {
            if (response.statusCode != HttpURLConnection.HTTP_OK) {
                throw new Exception(response.getResponseBodyAsString())
            }
        }
    })
}

def getPage = { spaceKey, pageTitle ->
    def paramsPage = [
            "spaceKey": spaceKey,
            "title": pageTitle
    ]

    //Get the confluence page
    authenticatedRequestFactory
            .createRequest(Request.MethodType.GET, "rest/api/content/?${buildURLParams(paramsPage)}")
            .addHeader("Content-Type", "application/json")
            .addHeader('Authorization', 'Basic ' + 'admin:admin'.bytes.encodeBase64().toString())
            .executeAndReturn(new ReturningResponseHandler<Response, String>() {
        @Override
        String handle(Response response) throws ResponseException {
            if (response.statusCode != HttpURLConnection.HTTP_OK) {
                throw new Exception(response.getResponseBodyAsString())
            }
            return response.getResponseBodyAsString()
        }
    })
}

def buildURLParams (def urlParams ) {
    return urlParams.collect {key, String value ->
        def encodedValue = URLEncoder.encode(value, "UTF-8")
        "$key=$encodedValue"
    }.join("&")
}

def returnObject = new JsonSlurper().parseText(getPage("TEST1", "Test v1 Home"))
return returnObject
//createSpace("TEST1", "Test v1", "A description")
//createPage("TEST1", "Test Confluence page v1", "Hello world")