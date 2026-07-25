package com.superhumans.model.user;

public record SignUp (Integer id,
                      String firstName,
                      String lastName,
                      String middleName,
                      String login,
                      char[] password,
                      String userRole,
                      String businessRole) { }
