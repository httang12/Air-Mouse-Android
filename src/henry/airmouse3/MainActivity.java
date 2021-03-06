/********************************************************************************************
 *	Copyright(C) 2014  Enric del Molino 													*
 *	http://www.androidairmouse.com															*
 *	enricdelmolino@gmail.com																*
 *																							*
 *	This file is part of Air Mouse Client for Android.										*
 *																							*
 *   Air Mouse Client for Android is free software: you can redistribute it and/or modify	*
 *   it under the terms of the GNU General Public License as published by					*
 *   the Free Software Foundation, either version 3 of the License, or						*
 *   (at your option) any later version.														*
 *																							*
 *   Air Mouse Client for Android is distributed in the hope that it will be useful,			*
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of							*
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the							*
 *   GNU General Public License for more details.											*
 *																							*
 *   You should have received a copy of the GNU General Public License						*
 *   along with Air Mouse Server for Android.  If not, see <http://www.gnu.org/licenses/>.	*
 *********************************************************************************************/

package henry.airmouse3;

import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class MainActivity extends Activity {

	ImageView _buttonLeft, _buttonRight, _buttonWheel, _buttonFocus;
	private boolean _startActivity = false;
	private boolean _wheelScsrolling = false;
	private boolean _focusOn = false;
	private boolean _anyKeyPressedWhileFocusOn = false;
	private float _lastWheelPixel = -1;
	private byte _wheelStep = 0;

	private OrientationEventListener myOrientationEventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		_buttonLeft = (ImageView) findViewById(R.id.imageViewButtonLeft);
		_buttonLeft.setOnTouchListener(OnTouchButtonLeft);

		_buttonRight = (ImageView) findViewById(R.id.imageViewButtonRight);
		_buttonRight.setOnTouchListener(OnTouchButtonRight);

		_buttonWheel = (ImageView) findViewById(R.id.imageViewButtonWheel);
		_buttonWheel.setOnTouchListener(OnTouchWheel);

		_buttonFocus = (ImageView) findViewById(R.id.imageViewButtonFocus);
		_buttonFocus.setOnTouchListener(OnTouchButtonFocus);

		myOrientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {

			@Override
			public void onOrientationChanged(int arg0) {
				if (arg0 != -1) {
					//Log.i("Log Scan", String.valueOf(arg0));
					//Connection.Send(String.valueOf(arg0)); /*Just for debugging*/
					if (arg0 > 85 && arg0 < 95) {
						_startActivity = true;
						Intent intent = new Intent(getApplicationContext(), KeyboardActivity.class);
						intent.putExtra("reverse", true);
						startActivity(intent);
					}
					if (arg0 > 265 && arg0 < 275) {
						_startActivity = true;
						Intent intent = new Intent(getApplicationContext(), KeyboardActivity.class);
						intent.putExtra("reverse", false);
						startActivity(intent);
					}
				}
			}
		};

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (Settings.getFISRT_USE()) {
			AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
			myAlertDialog.setTitle(R.string.app_name);
			myAlertDialog.setMessage("Calibration sensor needs to be calibrated, do you want to calibrate now? just place your phone over a flat surface and click OK");
			myAlertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface arg0, int arg1) {
					MotionProvider.Calibrate();
					Settings.setFISRT_USE(false);
				}
			});
			myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface arg0, int arg1) {

				}
			});
			myAlertDialog.show();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		AssignMotionListeners();

	}

	@Override
	protected void onPause() {
		super.onPause();
		ReleaseMotionListeners();
		if (!_startActivity) {
			finish();
		}
		// wakelock.release();

	};

	@Override
	public void onSaveInstanceState(Bundle icicle) {
		super.onSaveInstanceState(icicle);
		// wakelock.release();
	}

	@Override
	protected void onStop() {

		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Connection.Disconnect();
		super.onDestroy();
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
		getMenuInflater().inflate(R.layout.menugyro, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_calibrate:
			MotionProvider.Calibrate();
			return true;

		case R.id.menu_settings:
			_startActivity = true;
			Intent Intent = new Intent(this, SettingsActivity.class);
			startActivity(Intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void AssignMotionListeners() {
		if (myOrientationEventListener != null && myOrientationEventListener.canDetectOrientation()) {
			myOrientationEventListener.enable();
		}

		MotionProvider.SetOnMotionChanged(OnMotionChanged);
		MotionProvider.SetOnCalibrationFinished(OnCalibrationFinished);
		MotionProvider.RegisterEvents(getApplicationContext());
	}

	private void ReleaseMotionListeners() {
		if (myOrientationEventListener != null) {
			myOrientationEventListener.disable();
		}
		MotionProvider.ReleaseCalibrationListener();
		MotionProvider.ReleaseMotionListener();
		MotionProvider.UnregisterEvents(getApplicationContext());
	}

	private OnCalibrationFinishedListener OnCalibrationFinished = new OnCalibrationFinishedListener() {

		@Override
		public void OnCalibrationFinished() {
			ShowToast("Calibration OK");
		}
	};

	private OnMotionChangedListener OnMotionChanged = new OnMotionChangedListener() {

		@Override
		public void OnMotionChanged(float x, float y) {

			float motionFactor = Settings.getMOTION_FACTOR();
			if (_focusOn)
				motionFactor /= 8;

			float finalX = x * motionFactor;
			float finalY = y * motionFactor;

			if (Math.abs(finalX) > Settings.getMIN_MOVEMENT() || Math.abs(finalY) > Settings.getMIN_MOVEMENT()) {
				Connection.Send(Commands.GetMouseDeltaString(finalX, finalY));
			}
		}
	};

	private void ShowToast(final String str) {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
			}
		});
	}

	private OnTouchListener OnTouchWheel = new OnTouchListener() {

		//@TargetApi(Build.VERSION_CODES.GINGERBREAD)
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (_focusOn) {
				_anyKeyPressedWhileFocusOn = true;
			}

			if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
				Drawable clickedDrawable = getResources().getDrawable(R.drawable.midbuttonclick);
				_buttonWheel.setImageDrawable(clickedDrawable);

			} else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {

				// Wheel Move End
				if (_wheelScsrolling) {
					_lastWheelPixel = -1;
					_wheelScsrolling = false;
					Drawable clickedDrawable = getResources().getDrawable(R.drawable.midbutton);
					_buttonWheel.setImageDrawable(clickedDrawable);
				}

				// Click wheel
				else {
					Connection.Send(Commands.WheelUp);
					Drawable clickedDrawable = getResources().getDrawable(R.drawable.midbutton);
					_buttonWheel.setImageDrawable(clickedDrawable);
				}

			} else if (event.getAction() == android.view.MotionEvent.ACTION_MOVE) {

				PointerCoords coordinates = new PointerCoords();
				event.getPointerCoords(event.getPointerCount() - 1, coordinates);
				if (_lastWheelPixel == -1) {
					_lastWheelPixel = coordinates.y;
				}

				else if (Math.abs(coordinates.y - _lastWheelPixel) > Settings.getMIN_WHEEL_PIXELS()) {
					float delta = coordinates.y - _lastWheelPixel;
					Connection.Send(Commands.GetWheelDeltaString(delta));
					_lastWheelPixel = coordinates.y;
					_wheelScsrolling = true;

					if (_wheelStep == 0) {
						if (delta < 0) {
							Drawable clickedDrawable = getResources().getDrawable(R.drawable.midbuttonup1);
							_buttonWheel.setImageDrawable(clickedDrawable);
						} else {
							Drawable clickedDrawable = getResources().getDrawable(R.drawable.midbuttondown1);
							_buttonWheel.setImageDrawable(clickedDrawable);
						}
					}
					if (_wheelStep == 1) {
						if (delta < 0) {
							Drawable clickedDrawable = getResources().getDrawable(R.drawable.midbuttonup2);
							_buttonWheel.setImageDrawable(clickedDrawable);
						} else {
							Drawable clickedDrawable = getResources().getDrawable(R.drawable.midbuttondown2);
							_buttonWheel.setImageDrawable(clickedDrawable);
						}
					}
					if (_wheelStep == 2) {
						if (delta < 0) {
							Drawable clickedDrawable = getResources().getDrawable(R.drawable.midbuttonup3);
							_buttonWheel.setImageDrawable(clickedDrawable);
						} else {
							Drawable clickedDrawable = getResources().getDrawable(R.drawable.midbuttondown3);
							_buttonWheel.setImageDrawable(clickedDrawable);
						}
					}

					if (_wheelStep == 2)
						_wheelStep = 0;
					else
						_wheelStep++;

				}
			}
			return true;
		}
	};

	private OnTouchListener OnTouchButtonRight = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (_focusOn) {
				_anyKeyPressedWhileFocusOn = true;
			}

			if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
				SendRightDown();

			} else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
				SendRightUp();
			}
			return true;
		}
	};

	private OnTouchListener OnTouchButtonLeft = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (_focusOn) {
				_anyKeyPressedWhileFocusOn = true;
			}

			if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
				SendLeftDown();
			} else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
				SendLeftUp();
			}
			return true;
		}
	};

	private OnTouchListener OnTouchButtonFocus = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
				Drawable clickedDrawable = getResources().getDrawable(R.drawable.focusclick);
				_buttonFocus.setImageDrawable(clickedDrawable);
				_focusOn = true;

			} else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {

				if (!_anyKeyPressedWhileFocusOn) {
					if (Settings.getSWITCH_BUTTONS()) {
						Connection.Send(Commands.DownRight);
						Connection.Send(Commands.UpRight);
					} else {
						Connection.Send(Commands.DownLeft);
						Connection.Send(Commands.UpLeft);
					}
				} else {
					_anyKeyPressedWhileFocusOn = false;
				}
				Drawable clickedDrawable = getResources().getDrawable(R.drawable.focus);
				_buttonFocus.setImageDrawable(clickedDrawable);
				_focusOn = false;
			}
			return true;
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (Settings.getSWITCH_VOLUME_BUTTONS_RAISE_CLICK()) {
				SendLeftDown();
			} else {
				SendVolumeDown();
			}
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			if (Settings.getSWITCH_VOLUME_BUTTONS_RAISE_CLICK()) {
				SendRightDown();
			} else {
				SendVolumeUp();
			}
		} else {
			return super.onKeyDown(keyCode, event);
		}

		return keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (Settings.getSWITCH_VOLUME_BUTTONS_RAISE_CLICK()) {
				SendLeftUp();
			}
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			if (Settings.getSWITCH_VOLUME_BUTTONS_RAISE_CLICK()) {
				SendRightUp();
			}
		} else {
			return super.onKeyUp(keyCode, event);
		}
		return keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
	};

	private void SendVolumeUp() {
		Connection.Send(Commands.VolumeUp);
	}

	private void SendVolumeDown() {
		Connection.Send(Commands.VolumeDown);
	}

	private void SendLeftUp() {
		if (Settings.getSWITCH_BUTTONS())
			Connection.Send(Commands.UpRight);
		else
			Connection.Send(Commands.UpLeft);

		Drawable clickedDrawable = getResources().getDrawable(R.drawable.leftbutton);
		_buttonLeft.setImageDrawable(clickedDrawable);
	}

	private void SendLeftDown() {

		if (Settings.getSWITCH_BUTTONS())
			Connection.Send(Commands.DownRight);
		else
			Connection.Send(Commands.DownLeft);

		Drawable clickedDrawable = getResources().getDrawable(R.drawable.leftbuttonclick);
		_buttonLeft.setImageDrawable(clickedDrawable);
	}

	private void SendRightUp() {
		if (Settings.getSWITCH_BUTTONS())
			Connection.Send(Commands.UpLeft);
		else
			Connection.Send(Commands.UpRight);

		Drawable clickedDrawable = getResources().getDrawable(R.drawable.rightbutton);
		_buttonRight.setImageDrawable(clickedDrawable);
	}

	private void SendRightDown() {
		if (Settings.getSWITCH_BUTTONS())
			Connection.Send(Commands.DownLeft);
		else
			Connection.Send(Commands.DownRight);

		Drawable clickedDrawable = getResources().getDrawable(R.drawable.rightbuttonclick);
		_buttonRight.setImageDrawable(clickedDrawable);
	}

}