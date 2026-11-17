package com.example.tool_calling_confirmation;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class DeploymentTools {


    @Tool(description = "Deploy a service to the DEV environment. Use this when the user asks to deploy, release, or ship something to dev or development.")
    public String deployToDev(
            @ToolParam(description = "Name of the service to deploy, for example billing-api") String serviceName) {
        System.out.println("Deploying " + serviceName + " to DEV");
        return "Deployed " + serviceName + " to DEV";
    }

    @Tool(description = "Deploy a service to the UAT environment. Use this when the user asks to deploy," +
            " release, or promote something to uat or user acceptance testing.")
    public String deployToUat(
            @ToolParam(description = "Name of the service to deploy, for example billing-api") String serviceName) {
        System.out.println("Deploying " + serviceName + " to UAT");
        return "Deployed " + serviceName + " to UAT";
    }

    @Tool(description = "Deploy a service to the PROD environment. Use this when the user asks" +
            " to deploy, release, or ship something to prod or production." ,requiresConfirmation = true)
    public String deployToProd(
            @ToolParam(description = "Name of the service to deploy, for example billing-api") String serviceName
           ) {
        System.out.println("Deploying " + serviceName + " to PROD");
        return "Deployed " + serviceName + " to PROD";
    }


}
