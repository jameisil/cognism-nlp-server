/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cognism.webservices;

/**
 *
 * @author Home
 */
public class ResponseStatus {

    public static int INVALID_JSON = 101;
    public static int RECORD_SAVED = 200;
    public static int COGNITIVE_ALREADY_EXIST = 700;
    public static String JSON_IS_NULL = "JSON_IS_NULL";
    public static String JSON_IS_INVALID = "JSON_IS_INVALID";
    public static String FILE_NAME_IS_NOT_DEFINED = "FILE_NAME_IS_NOT_DEFINED";
    public static String SERVER_IS_NOT_DEFINED = "SERVER_IS_NOT_DEFINED";
    public static String JSON_PARSE_EXCEPTION = "JSON_PARSE_EXCEPTION";
}
