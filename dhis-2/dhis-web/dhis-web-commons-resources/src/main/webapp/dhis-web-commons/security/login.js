
var login = {};
login.localeKey = "dhis2.locale.ui";

$( document ).ready( function()
{

	drawComplexCaptcha();
	//Disable cut copy paste
	$('body').bind('cut copy paste', function (e) {
		e.preventDefault();
	});

	//Disable mouse right click
	$("body").on("contextmenu",function(e){
		return false;
	});

	$( '#j_username' ).focus();

	var checked = document.getElementById( '2fa' ).checked;

	$( '#2fa' ).click( function () {
		$( '#2fa_code' ).attr("hidden", checked);
		$( '#2fa_code' ).attr("readonly", checked);
		document.getElementById( '2fa' ).checked = !checked;

		checked = !checked;
	});

	$( '#loginForm').bind( 'submit', function()
	{
		if ( window.location.hash )
		{
			$(this).prop('action', $(this).prop('action') + window.location.hash );
		}

		$( '#submit' ).attr( 'disabled', 'disabled' );

		sessionStorage.removeItem( 'ouSelected' );
		sessionStorage.removeItem( 'USER_PROFILE' );
		sessionStorage.removeItem( 'USER_SETTING' );
		sessionStorage.removeItem( 'eventCaptureGridColumns');
		sessionStorage.removeItem( 'trackerCaptureGridColumns');
		sessionStorage.removeItem( 'trackerCaptureCategoryOptions');
		sessionStorage.removeItem( 'eventCaptureCategoryOptions');
	} );

	var locale = localStorage[login.localeKey];

	if ( undefined !== locale && locale )
	{
		login.changeLocale( locale );
		$( '#localeSelect option[value="' + locale + '"]' ).attr( 'selected', 'selected' );
	}
} );

login.localeChanged = function()
{
	var locale = $( '#localeSelect :selected' ).val();

	if ( locale )
	{
		login.changeLocale( locale );
		localStorage[login.localeKey] = locale;
	}
}

login.changeLocale = function( locale )
{
	$.get( 'loginStrings.action?keyApplication=Y&loc=' + locale, function( json ) {
		$( '#createAccountButton' ).html( json.create_an_account );
		$( '#signInLabel' ).html( json.sign_in );
		$( '#j_username' ).attr( 'placeholder', json.login_username );
		$( '#j_password' ).attr( 'placeholder', json.login_password );
		$( '#2fa_code' ).attr( 'placeholder', json.login_code );
		$( '#forgotPasswordLink' ).html( json.forgot_password );
		$( '#createAccountLink' ).html( json.create_an_account );
		$( '#loginMessage' ).html( json.wrong_username_or_password );
		$( '#poweredByLabel' ).html( json.powered_by );
		$( '#submit' ).val( json.sign_in );

		$( '#titleArea' ).html( json.applicationTitle );
		$( '#introArea' ).html( json.keyApplicationIntro );
		$( '#notificationArea' ).html( json.keyApplicationNotification );
		$( '#applicationFooter' ).html( json.keyApplicationFooter );
		$( '#applicationRightFooter' ).html( json.keyApplicationRightFooter );
	} );
}


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




// Validate the Entered input aganist the generated security code function

/*
function validCaptcha()
{
	var str1 = removeSpaces(document.getElementById('txtCaptcha').value);
	var str2 = removeSpaces(document.getElementById('txtCaptchaInput').value);
	if (str1 == str2)
	{
		return true;
	}
	return false;
}
*/

// Remove the spaces from the entered and generated code
function removeSpaces(string)
{
	return string.split(' ').join('');
}

function captchaValidations()
{
	if(grecaptcha && grecaptcha.getResponse().length > 0)
	{
		//the recaptcha is checked
		return true;
	}
	else
	{
		//The recaptcha is not cheched
		alert('You have to check the recaptcha !');
		return false;
	}
}

