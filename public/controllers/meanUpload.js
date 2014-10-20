'use strict';

angular.module('mean.upload').controller('MeanUploadController', ['$scope', 'Global', 'MeanUpload',
  function($scope, Global, MeanUpload) {
    $scope.global = Global;
    $scope.images = [];

    $scope.package = {
        name: 'mean-upload'
    };

    $scope.images = [];


   $scope.deleteImage = function() {
      $scope.images = [];
      $scope.errorMessages = " ";
     $scope.slides = [];
     };


    $scope.uploadFileCallback = function(file) {
    $scope.errorMessages = [];
       console.log('length images'+ $scope.images.length);


      if ($scope.images.length === 0 && file.type.indexOf('image') !== -1) {
          $scope.errorMessages = " ";
          $scope.images.push(file);
          $scope.addSlide(file.src);
          }
      else if ($scope.images.length === 1 && file.type.indexOf('image') !== -1) {
          $scope.errorMessages.push('More Than One Image Not Allowed');
          } else {
            $scope.errorMessages.push('File Type Not Allowed');
             $scope.images = []
                  }

   console.log('length images at exit'+ $scope.images.length);
    };

    $scope.uploadFinished = function(files) {
      console.log(files);
    };

    $scope.myInterval = 5000;
    var slides = $scope.slides = [];
    $scope.addSlide = function(url) {
//           var newWidth = 600 + slides.length;
       slides.push({
         image: url
       });
    };
  }
]);
