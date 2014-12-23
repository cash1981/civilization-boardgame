﻿(function (module) {

  var USERKEY = "authorizationEncoded";

  var currentUser = function (localStorage, $log) {

    var saveUser = function () {
      localStorage.add(USERKEY, profile);
    };

    var removeUser = function () {
      localStorage.remove(USERKEY);
    };

    var initialize = function () {
      var user = {
        username: "",
        password: "",
        authorizationEncoded: "",
        get loggedIn() {
          $log.info("Checking if user is logged in " + this.authorizationEncoded);
          return this.authorizationEncoded ? true : false;
        }
      };

      var localUser = localStorage.get(USERKEY);
      if (localUser) {
        user.username = localUser.username;
        user.password = localUser.password;
        user.authorizationEncoded = localUser.authorizationEncoded;
      }
      return user;
    };

    var profile = initialize();

    return {
      save: saveUser,
      remove: removeUser,
      profile: profile
    };
  };

  module.factory("currentUser", currentUser);

}(angular.module("civApp")));
