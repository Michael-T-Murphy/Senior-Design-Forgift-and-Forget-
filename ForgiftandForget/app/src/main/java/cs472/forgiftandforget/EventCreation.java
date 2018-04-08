package cs472.forgiftandforget;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.TimeZone;

import cs472.forgiftandforget.DatabaseClasses.Event;

public class EventCreation extends AppCompatActivity {
	EditText eventField;
	EditText dateField;
	EditText timeField;
	private String eventListID;
	private String friendID;
	final static int TIME_PICK = 1;
	final static int DATE_PICK = 0;
	final static int MAX_HOURS = 12;
	final static int TEN = 10;
	int year;
	int month;
	int day;
	int hour;
	int minute;
	Boolean timeSet = false;
	Boolean dateSet = false;
	Boolean returningFromCalendar = false;
	int calendarID = 0;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (returningFromCalendar) {
			returningFromCalendar = false;
			ReturnToFriendList();
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_event_creation);

		Uri uri = CalendarContract.Calendars.CONTENT_URI;
		try {
			Cursor calendarCursor = managedQuery(uri, null, null, null, null);

			int indexPrimary = calendarCursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY);
			int indexID = calendarCursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID);
			while (calendarCursor.moveToNext()) {
				if (calendarCursor.getInt(indexPrimary) == 1) {
					calendarID = calendarCursor.getInt(indexID);
				}
			}
		}catch(Exception e) {
			// my device(ZTE Axon7) has no isPrimary column in the table, but id of 1 works for me
			calendarID = 1;
		}


		eventListID = getIntent().getStringExtra("ELID");
		friendID = getIntent().getStringExtra("FID");

		eventField = (EditText) findViewById(R.id.event);
		dateField = (EditText) findViewById(R.id.date);
		timeField = (EditText) findViewById(R.id.time);
		Calendar cal = Calendar.getInstance();
		year = cal.get(Calendar.YEAR);
		month = cal.get(Calendar.MONTH);
		day = cal.get(Calendar.DAY_OF_MONTH);


		dateField.setInputType(InputType.TYPE_NULL);
		timeField.setInputType(InputType.TYPE_NULL);
		dateField.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DATE_PICK);
			}
		});
		dateField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showDialog(DATE_PICK);
				}
			}
		});
		timeField.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(TIME_PICK);
			}
		});
		timeField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showDialog(TIME_PICK);
				}
			}
		});


	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == 0) {
			return new DatePickerDialog(this, datePickerListener, year, month, day);
		} else if (id == 1) {
			return new TimePickerDialog(this, timePickerListener, hour, minute, false);
		}
		return null;
	}

	private DatePickerDialog.OnDateSetListener datePickerListener = new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int chosenYear, int chosenMonth, int chosenDay) {
			year = chosenYear;
			month = chosenMonth + 1;
			day = chosenDay;
			String setDate = month + "/" + day + "/" + year;
			dateField.setText(setDate);
			dateSet = true;
		}
	};

	private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfHour) {
			hour = hourOfDay;
			int hourFixed = hour;
			minute = minuteOfHour;
			String amOrPm = "am";
			String setTime;

			if (hourFixed == 0) {
				hourFixed = 12;
			} else if (hourFixed > 12) {
				hourFixed -= 12;
				amOrPm = "pm";
			}
			if (minute < TEN) {
				setTime = hourFixed + ":0" + minute + amOrPm;
			} else {
				setTime = hourFixed + ":" + minute + amOrPm;
			}
			timeField.setText(setTime);
			timeSet = true;
		}
	};

	public void addNewEvent(View view) {
		if (!(dateSet && timeSet)) {
			Toast.makeText(getApplicationContext(), "Please set date and time first", Toast.LENGTH_LONG).show();
			return;
		}
		Event newEvent = new Event(eventField.getText().toString(), dateField.getText().toString());
		Event.AddEvent(eventListID, friendID, newEvent);
		Toast.makeText(getApplicationContext(), "Adding " + newEvent.name + " to events", Toast.LENGTH_LONG).show();

		Calendar startTime = Calendar.getInstance();
		startTime.set(year, month - 1, day, hour, minute);
		Calendar endTime = Calendar.getInstance();
		endTime.set(year, month - 1, day, hour, minute);

		returningFromCalendar = true;
		ContentResolver cr = getContentResolver();
		ContentValues values = new ContentValues();
		TimeZone timeZone = TimeZone.getDefault();
		values.put(CalendarContract.Events.DTSTART, startTime.getTimeInMillis());
		values.put(CalendarContract.Events.DTEND, endTime.getTimeInMillis());
		values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
		values.put(CalendarContract.Events.TITLE, eventField.getText().toString());
		values.put(CalendarContract.Events.DESCRIPTION, eventField.getText().toString());
		values.put(CalendarContract.Events.CALENDAR_ID, calendarID);
		values.put(CalendarContract.Events.HAS_ALARM, 1);
		Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
		String calendarEventID = uri.getLastPathSegment();


		Uri REMINDERS_URI = Uri.parse("content://com.android.calendar/reminders");
		values = new ContentValues();
		values.put( "event_id", calendarEventID);
		values.put( "method", 1 );
		values.put( "minutes", 1 );
		getContentResolver().insert( REMINDERS_URI, values );

		Intent intent = new Intent(EventCreation.this, FriendList.class);
		finish();
		startActivity(intent);
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent(EventCreation.this, FriendList.class);
			finish();
			startActivity(intent);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (returningFromCalendar) {
			returningFromCalendar = false;
			ReturnToFriendList();
		}
	}

	private void ReturnToFriendList() {
		Intent intent = new Intent(EventCreation.this, FriendList.class);
		finish();
		startActivity(intent);
	}
}
