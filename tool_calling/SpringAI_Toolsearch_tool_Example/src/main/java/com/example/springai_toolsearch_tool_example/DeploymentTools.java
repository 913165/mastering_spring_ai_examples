package com.example.springai_toolsearch_tool_example;

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

    @Tool(description = "Deploy a service to the UAT environment. Use this when the user asks to deploy, release, or promote something to uat or user acceptance testing.")
    public String deployToUat(
            @ToolParam(description = "Name of the service to deploy, for example billing-api") String serviceName) {
        System.out.println("Deploying " + serviceName + " to UAT");
        return "Deployed " + serviceName + " to UAT";
    }

    @Tool(description = "Deploy a service to the PROD environment. Use this when the user asks to deploy, release, or ship something to prod or production.")
    public String deployToProd(
            @ToolParam(description = "Name of the service to deploy, for example billing-api") String serviceName) {
        System.out.println("Deploying " + serviceName + " to PROD");
        return "Deployed " + serviceName + " to PROD";
    }

    @Tool(description = "Roll back a service in the DEV environment. Use this when the user asks to rollback, revert, or undo a deploy in dev or development.")
    public String rollbackFromDev(
            @ToolParam(description = "Name of the service to roll back, for example billing-api") String serviceName) {
        System.out.println("Rolling back " + serviceName + " in DEV");
        return "Rolled back " + serviceName + " in DEV";
    }

    @Tool(description = "Roll back a service in the UAT environment. Use this when the user asks to rollback, revert, or undo a deploy in uat or user acceptance testing.")
    public String rollbackFromUat(
            @ToolParam(description = "Name of the service to roll back, for example billing-api") String serviceName) {
        System.out.println("Rolling back " + serviceName + " in UAT");
        return "Rolled back " + serviceName + " in UAT";
    }

    @Tool(description = "Roll back a service in the PROD environment. Use this when the user asks to rollback, revert, or undo a deploy in prod or production.")
    public String rollbackFromProd(
            @ToolParam(description = "Name of the service to roll back, for example billing-api") String serviceName) {
        System.out.println("Rolling back " + serviceName + " in PROD");
        return "Rolled back " + serviceName + " in PROD";
    }
}
