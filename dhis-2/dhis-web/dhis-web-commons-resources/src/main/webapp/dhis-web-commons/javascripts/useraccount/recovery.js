var login = {};
login.localeKey = "dhis2.locale.ui";

$(document).ready(function() {

  var locale = localStorage[login.localeKey];

  if( undefined !== locale && locale ) {
    login.changeLocale(locale);
  }
  drawComplexCaptcha();
  //Disable cut copy paste
  $('body').bind('cut copy paste', function (e) {
    e.preventDefault();
  });

  //Disable mouse right click
  $("body").on("contextmenu",function(e){
    return false;
  });

});

function recoverAccount() {
  //alert( "inside captch validation");
  var username = $.trim($("#username").val());

  if(grecaptcha && grecaptcha.getResponse().length > 0)
  {
    //the recaptcha is checked
    if( username.length == 0 ) {
      return false;
    }

    $.ajax({
      url: "../../api/account/recovery",
      data: {
        username: username
      },
      type: "post",
      success: function(data) {
        $("#recoveryForm").hide();
        $("#recoverySuccessMessage").fadeIn();
      },
      error: function(data) {
        $("#recoveryForm").hide();
        $("#recoveryErrorMessage").fadeIn();
      }
    });
    return true;
  }
  else
  {
    //The recaptcha is not cheched
    alert('You have to check the recaptcha !');
    return false;
  }

}

login.changeLocale = function(locale) {
  $.get('recoveryStrings.action?loc=' + locale, function(json) {
    $('#accountRecovery').html(json.account_recovery);
    $('#labelUsername').html(json.user_name);
    $('#recoveryButton').val(json.recover);
    $('#recoverySuccessMessage').html(json.recover_success_message);
    $('#recoveryErrorMessage').html(json.recover_error_message);
  });
};


function drawCaptcha()
{
  var a = Math.ceil(Math.random() * 10)+ '';
  var b = Math.ceil(Math.random() * 10)+ '';
  var c = Math.ceil(Math.random() * 10)+ '';
  var d = Math.ceil(Math.random() * 10)+ '';
  var e = Math.ceil(Math.random() * 10)+ '';
  var f = Math.ceil(Math.random() * 10)+ '';
  var g = Math.ceil(Math.random() * 10)+ '';
  var code = a + ' ' + b + ' ' + ' ' + c + ' ' + d + ' ' + e + ' '+ f + ' ' + g;
  document.getElementById("txtCaptcha").value = code;
  document.getElementById("txtCaptchaInput").value = "";
}

function drawComplexCaptcha()
{
  var alpha = [ 'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
    '0','1','2','3','4','5','6','7','8','9',
    'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z']


  var a = alpha[Math.floor(Math.random()*62)];
  var b = alpha[Math.floor(Math.random()*62)];
  var c = alpha[Math.floor(Math.random()*62)];
  var d = alpha[Math.floor(Math.random()*62)];
  var e = alpha[Math.floor(Math.random()*62)];
  var f = alpha[Math.floor(Math.random()*62)];

  var code = a + b  + c + d + e + f;
  document.getElementById("txtCaptcha").value = code;
  document.getElementById("txtCaptchaInput").value = "";
}





// Remove the spaces from the entered and generated code
function removeSpaces(string)
{
  return string.split(' ').join('');
}


