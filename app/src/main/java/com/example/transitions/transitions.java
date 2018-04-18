// *********************************************************************************************************************
// ***                                                                                                               ***
// ***                Transitions                                                                                    ***
// ***                                                                                                               ***
// *** TWiG Software Services, by Mark James Capella                                                                 ***
// ***                                                                                                               ***
// ***    From my original book : Games Apples Play (c) 1983                                                         ***
// ***                                                                                                               ***
// ***    1    - Game project copied from Twonky skeleton, background, title, buttons designed                       ***
// ***    1.1  - Quick backup adding button code                                                                     ***
// ***    1.2  - Mostly Done! need to add routine to determine when player stuck / end of game                       ***
// ***           Need to figure out post-install / multi instance cold-start bug                                     ***
// ***           System quirk? Seems to affect Twonky also ---v                                                      ***
// ***    1.21 - Added android:launchMode="singleInstance" to prevent multiple instances confusion after install     ***
// ***    1.3  - Game complete! Maybe 1.31 will have a graceful restart option                                       ***
// ***    1.31 - Added graceful game over / play again option                                                        ***
// ***           ISSUE: I CAN'T FIND HOW TO WIN THIS GAME ! DID THIS NEVER WORK IN THE ORIGINAL AND                  ***
// ***                  NOBODY NOTICED?                                                                              ***
// ***    2.1  - proved it works ! Added checkbox to startup / help dialog                                           ***
// ***    2.2  - added onDestroy() logic to dismiss any active alertDialogs to avoid logcat memory leak messages     ***
// ***                                                                                                               ***
// ***           Outstanding issue: if showing gameIsWon dialog (for example) and you rotate screen                  ***
// ***           the dialog is dismissed and you lose the handy Play Again? functionality ... note that back in      ***
// ***           API 3 the dialog stayed on the screen                                                               ***
// ***                                                                                                               ***
// ***    2.3  - Added logic to preserve gameIsLost/Won alerts across screen rotations ... help, help2, and          ***
// ***           aboutAlerts just continue to dismiss gracefully :)                                                  ***
// ***                                                                                                               ***
// *********************************************************************************************************************

package com.example.transitions;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

import android.os.Bundle;

import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;


// *********************************************************************************************************************
// ***                                                                                                               ***
// *** Main game activity, definitions, etc                                                                          ***
// ***                                                                                                               ***
// *********************************************************************************************************************

public class transitions extends Activity {

	// who we are
   final Context context = this;

   // debug flag, set to true to enable toast / flow / status messages
   public static final boolean twigDebug = true;

   // Board and related
   final static int BOARD_SIZE = 9; // Game Board Size
   final static int BOARD_EMPTY = 0;
   final static int BOARD_PURPLE = 1;
   final static int BOARD_GREEN = 2;

   final static int BOARD_START [] = {BOARD_PURPLE, BOARD_PURPLE, BOARD_PURPLE, BOARD_PURPLE,
   											  BOARD_EMPTY,
   											  BOARD_GREEN, BOARD_GREEN, BOARD_GREEN, BOARD_GREEN};
   final static int BOARD_END []   = {BOARD_GREEN, BOARD_GREEN, BOARD_GREEN, BOARD_GREEN,
   											  BOARD_EMPTY,
   											  BOARD_PURPLE, BOARD_PURPLE, BOARD_PURPLE, BOARD_PURPLE,};
   final static int BOARD_TEST []  = {BOARD_GREEN, BOARD_GREEN, BOARD_GREEN, BOARD_EMPTY,
   											  BOARD_GREEN,
   											  BOARD_PURPLE, BOARD_PURPLE, BOARD_PURPLE, BOARD_PURPLE,};

   private int[] persBoard = new int[BOARD_SIZE];

   // Define persistent data from time the utility is installed
   final static boolean init_helpFlg = true;
   private boolean pers_helpFlg;

	// Define persistent data from time the utility starts to time the utility ends
	final static int GAME_ACTIVE = 0;
	final static int GAME_LOST = 1;
	final static int GAME_WON = 2;

   final static int init_gameStatus = GAME_ACTIVE;
   int pers_gameStatus;

	AlertDialog aboutAlertDialog = null;
	AlertDialog helpAlertDialog  = null;
	AlertDialog help2AlertDialog = null;
	AlertDialog lostAlertDialog  = null;
	AlertDialog wonAlertDialog   = null;

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onCreate%28android.os.Bundle%29              ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
		log("onCreate()");

      setContentView(R.layout.transitions);
		onCreate_init (savedInstanceState);
    }

// *********************************************************************************************************************
// *** Disable the hardware back button, not needed for the game                                                     ***
// *********************************************************************************************************************

	public void onBackPressed() {
		log("onBackPressed()");
	}

// *********************************************************************************************************************
// *** Load persistent program values at onCreate, during cold or warm starts                                        ***
// *********************************************************************************************************************

   void onCreate_init(Bundle savedInstanceState) {
      log("onCreate()");

      if (savedInstanceState == null) {
         onCreate_init_cold();
			if (pers_helpFlg)
		      do_help();
      } else {
         onCreate_init_warm();
      }

	}

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** Initialize data that needs to be persistent across swap outs by Android Operating System                      ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onCreate_init_cold() {
		log("onCreate_init_cold()");

      SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);

		// Prefs reset first time game is run after install
      pers_helpFlg = prefs.getBoolean("pers_helpFlg", init_helpFlg);

      // Prefs reset every-time new game started
      pers_gameStatus = init_gameStatus;

      // Pre-clear the maze
      for (int i = 0; i < BOARD_SIZE; i++)
      	persBoard[i] = BOARD_START[i];

		displayBoard();
	}

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** Restore data that needs to be persistent across swap outs by Android Operating System                         ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onCreate_init_warm() {
		log("onCreate_init_warm()");

      SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);

      // Prefs reloaded every-time game resumes play
      pers_helpFlg = prefs.getBoolean("pers_helpFlg", init_helpFlg);
      pers_gameStatus = prefs.getInt("pers_gameStatus", init_gameStatus);

      for (int i = 0; i < BOARD_SIZE; i++)
         persBoard[i] = prefs.getInt("persBoard_" + i, BOARD_EMPTY);

		displayBoard();

		if (pers_gameStatus == GAME_LOST)
			alertGameIsLost();
		else
			if (pers_gameStatus == GAME_WON)
				alertGameIsWon();
   }

// *********************************************************************************************************************
// *** Display the board                                                                                             ***
// *********************************************************************************************************************

	public void displayBoard() {
		log("displayBoard()");

		String msg = "TransitionsBOARD ";
		for (int i = 0; i < BOARD_SIZE; i++) {
			msg += persBoard[i];
			setButtonImage(i);
		}

		log(msg);
	}

// *********************************************************************************************************************
// *** Inflate the Android standard hardware menu button View                                                        ***
// *********************************************************************************************************************

    public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenu()");

		getMenuInflater().inflate(R.menu.transitions, menu);
      return super.onCreateOptionsMenu(menu);
    }

// *********************************************************************************************************************
// *** Process user select menu View items                                                                           ***
// *********************************************************************************************************************

    public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected()");

        switch (item.getItemId()) {

            case R.id.menu_help:
                do_help();
                break;

            case R.id.menu_about:
                do_about();
                break;

            case R.id.menu_exit:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

// *********************************************************************************************************************
// *** Display / Process the About Screen menu View item                                                                           ***
// *********************************************************************************************************************

   void do_about() {
		log("do_about()");

      String aboutMsg = context.getString(R.string.app_name) + "\n";
      try {
         aboutMsg += getPackageManager().getPackageInfo(getPackageName(), 0).versionName + "\n";
      } catch (NameNotFoundException e) {
   	}
      aboutMsg += "\n" + context.getString(R.string.aboutalert_message);

      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
      alertDialogBuilder.setTitle(R.string.aboutalert_title)
              			   .setMessage(aboutMsg)
                			.setPositiveButton(R.string.aboutalert_button_OK,
                        						 new DialogInterface.OnClickListener() {
                            						 public void onClick(DialogInterface dialog, int id) { }
                        						 })
								.setCancelable(true);

		aboutAlertDialog = alertDialogBuilder.create();
		aboutAlertDialog.show();
   }

// *********************************************************************************************************************
// *** Display / Process 1 of 3 Help Screen menu View items                                                          ***
// *********************************************************************************************************************

	void do_help() {
		log("do_help()");

		// Setup Helpbox's checkbox and listener
		View checkBoxView = View.inflate(this, R.layout.checkbox, null);
		CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
		checkBox.setChecked(pers_helpFlg);
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
	   	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				pers_helpFlg = isChecked;
			}
		});

   	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
      alertDialogBuilder.setTitle(R.string.helpalert_title);
      alertDialogBuilder.setMessage(R.string.helpalert_message1)
      						.setView(checkBoxView)
                        .setNegativeButton(R.string.helpalert_button_MORE,
                                           new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
                                                 do_help2();
                                              }
                                           })
                        .setPositiveButton(R.string.helpalert_button_PLAY,
                                           new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) { }
                                           })
                        .setCancelable(true);

		helpAlertDialog = alertDialogBuilder.create();
		helpAlertDialog.show();
    }

// *********************************************************************************************************************
// *** Display / Process 2 of 3 Help Screen menu View items                                                          ***
// *********************************************************************************************************************

	void do_help2() {

		// Setup Helpbox's checkbox and listener
		View checkBoxView = View.inflate(this, R.layout.checkbox, null);
		CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
		checkBox.setChecked(pers_helpFlg);
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
	   	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				pers_helpFlg = isChecked;
			}
		});

   	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
      alertDialogBuilder.setTitle(R.string.helpalert_title);
      alertDialogBuilder.setMessage(R.string.helpalert_message2)
      						.setView(checkBoxView)
                        .setPositiveButton(R.string.helpalert_button_PLAY,
                                           new DialogInterface.OnClickListener() {
                                               public void onClick(DialogInterface dialog, int id) { }
                                           })
                        .setCancelable(true);

		help2AlertDialog = alertDialogBuilder.create();
		help2AlertDialog.show();
	}

// *********************************************************************************************************************
// *** Move the button selected if possible                                                                          ***
// *********************************************************************************************************************

	public void moveButton(View button) {
		log("moveButton()");

		int buttonIndex = indexOfView(button);
		int moveIndex = whereCanMoveTo(buttonIndex);

		// Check to see if the selected button can move/jump, if so, swap it's position, then
		// Check for win or lose situation
		if (buttonIndex != moveIndex) {
			performSwap(buttonIndex, moveIndex);
			seeIfGameOver();
		}
	}
	
// *********************************************************************************************************************
// *** Return where a specific button could move to                                                                  ***
// *********************************************************************************************************************

	public int whereCanMoveTo(int start) {
		log("whereCanMoveTo(" + start + ")");

		if (persBoard[start] == BOARD_EMPTY) {
			//Toast.makeText(context, "Can't move the empty space", Toast.LENGTH_SHORT).show();
			return start;
		}

		// Can the button simply move a position?
		int direction1 = persBoard[start] == BOARD_PURPLE ? 1 : -1;
		if (start + direction1 < 0 || start + direction1 >= BOARD_SIZE) {
			//Toast.makeText(context, "Can't move off", Toast.LENGTH_SHORT).show();
			return start;
		}

		if (persBoard[start + direction1] == BOARD_EMPTY) {
			// displayBoard();
			//Toast.makeText(context, "Can move", Toast.LENGTH_SHORT).show();
			return start + direction1;
		}

		// Can the button jump over into a position?
		int direction2 = persBoard[start] == BOARD_PURPLE ? 2 : -2;
		if (start + direction2 < 0 || start + direction2 >= BOARD_SIZE) {
			//Toast.makeText(context, "Can't jump off", Toast.LENGTH_SHORT).show();
			return start;
		}

		if (persBoard[start + direction2] == BOARD_EMPTY &&
			 persBoard[start + direction1] != persBoard[start]) {
			// displayBoard();
			//Toast.makeText(context, "Can jump", Toast.LENGTH_SHORT).show();
			return start + direction2;
		}

		//Toast.makeText(context, "Can't move or jump", Toast.LENGTH_SHORT).show();
		return start;
	}

// *********************************************************************************************************************
// *** Swap two button positions                                                                                     ***
// *********************************************************************************************************************

	public void performSwap(int start, int end) {
		log("performSwap()");

		int boardTemp = persBoard[start];
		persBoard[start] = persBoard[end];
		persBoard[end] = boardTemp;
		setButtonImage(start);
		setButtonImage(end);
	}

// *********************************************************************************************************************
// *** Translate a button view to an int (0 to BOARD_SIZE), -1 if (impossibly) button not found                      ***
// *********************************************************************************************************************

	public int indexOfView(View button) {
		log("indexOfView()");

		int buttonInt;
		try {
    		buttonInt = Integer.parseInt(String.valueOf(button.getId()));
		} catch(NumberFormatException e) {
   		log("(Could not parse) " + e);
   		return -1;
		}

		for (int i = 0; i < BOARD_SIZE; i++)
			if (buttonInt == context.getResources().getIdentifier("button" + String.valueOf(i), "id", getPackageName()))
			   return i;

	   return 0;
	}

// *********************************************************************************************************************
// *** Translate an int to a button view                                                                             ***
// *********************************************************************************************************************

	public void setButtonImage(int button) {
		log("setButtonImage()");

		View buttonView = viewOfIndex(button);
		//log("Called to draw button " + button);
		switch (persBoard[button])
		{
			case BOARD_PURPLE:
				buttonView.setBackgroundResource(R.drawable.buttonpurple64x64);
				break;
			case BOARD_EMPTY:
				buttonView.setBackgroundResource(R.drawable.buttontransparent64x64);
				break;
			case BOARD_GREEN:
				buttonView.setBackgroundResource(R.drawable.buttongreen64x64);
				break;
		}
	}

// *********************************************************************************************************************
// *** Translate an int to a button view                                                                             ***
// *********************************************************************************************************************

	public View viewOfIndex(int button) {
		log("viewOfIndex()");

		for (int i = 0; i < BOARD_SIZE; i++)
			if (button == i)
			   return (Button) findViewById(context.getResources().
			   						 getIdentifier("button" + String.valueOf(i), "id", getPackageName()));

		log("no indexOfView() found");
	   return (View) null;
	}

// *********************************************************************************************************************
// *** See if the game is over, by win or loss                                                                       ***
// *********************************************************************************************************************

	public void seeIfGameOver() {
		log("seeIfGameOver()");

		// If a single valid move exists, game continues
		for (int i = 0; i < BOARD_SIZE; i++) {
			int j = whereCanMoveTo(i);
			if (i != j)
				return;
		}

		// If board not in end game state, player loses, else wins
		for (int i = 0; i < BOARD_SIZE; i++)
			if (persBoard[i] != BOARD_END[i]) {
				pers_gameStatus = GAME_LOST;
				alertGameIsLost();
				return;
			}

		pers_gameStatus = GAME_WON;
		alertGameIsWon();
	}

// *********************************************************************************************************************
// *** Alert the player that he's lost                                                                               ***
// *********************************************************************************************************************

	public void alertGameIsLost() {
		log("alertGameIsLost()");

      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
      alertDialogBuilder.setTitle(R.string.gameIsLostAlert_title)
              			   .setMessage(R.string.gameIsLostAlert_message)
                        .setNegativeButton(R.string.gameIsLostAlert_button_NO,
                                           new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
																 finish();
                                              }
                                           })
                        .setPositiveButton(R.string.gameIsLostAlert_button_YES,
                                           new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
                                              	 onCreate_init_cold();
                                           }
                                           })
								.setCancelable(false);

		lostAlertDialog = alertDialogBuilder.create();
		lostAlertDialog.show();
	}

// *********************************************************************************************************************
// *** Alert the player that he's won                                                                                ***
// *********************************************************************************************************************

	public void alertGameIsWon() {
		log("alertGameIsWon()");

      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
      alertDialogBuilder.setTitle(R.string.gameIsWonAlert_title)
              			   .setMessage(R.string.gameIsWonAlert_message)
                        .setNegativeButton(R.string.gameIsWonAlert_button_NO,
                                           new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
                                              	 finish();
                                              }
                                           })
                        .setPositiveButton(R.string.gameIsWonAlert_button_YES,
                                           new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
                                              	 onCreate_init_cold();
															 }
														 })
								.setCancelable(false);

		wonAlertDialog = alertDialogBuilder.create();
		wonAlertDialog.show();
	}

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onRestart%28%29                              ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onRestart() {
   	super.onRestart();
      log("onRestart()");
   }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onStart%28%29                                ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onStart() {
      super.onStart();
      log("onStart()");
   }

// *********************************************************************************************************************
// ***                                                                                                               ***
// ***                                                                                                               ***
// ***                                                                                                               ***
// *********************************************************************************************************************

	public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
		log("onRestoreInstanceState()");
 	}

// *********************************************************************************************************************
// ***                                                                                                               ***
// ***  http://developer.android.com/reference/android/app/Activity.html#onResume%28%29                              ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onResume() {
      super.onResume();
      log("onResume()");
   }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onPause%28%29                                ***
// ***                                                                                                               ***
// *** Save data that needs to be persistent across swap outs by Android Operating System                            ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   protected void onPause() {
      super.onPause();
      log("onPause()");

      SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);
      SharedPreferences.Editor editor = prefs.edit();

      // prefs saved every time game paused during play
      editor.putBoolean("pers_helpFlg", pers_helpFlg);
      editor.putInt("pers_gameStatus",  pers_gameStatus);

      for (int i = 0; i < BOARD_SIZE; i++)
           editor.putInt("persBoard_" + i, persBoard[i]);


      editor.commit();
   }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onSaveInstanceState%28android.os.Bundle%29   ***
// ***                                                                                                               ***
// *********************************************************************************************************************

	public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
		log("onSaveInstanceState()");
	}

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onStop%28%29                                 ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onStop() {
      super.onStop();
      log("onStop()");
   }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onDestroy%28%29                              ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onDestroy() {

		if (aboutAlertDialog != null)
			aboutAlertDialog.dismiss();
		if (helpAlertDialog != null)
			helpAlertDialog.dismiss();
		if (help2AlertDialog != null)
			help2AlertDialog.dismiss();
		if (lostAlertDialog != null)
			lostAlertDialog.dismiss();
		if (wonAlertDialog != null)
			wonAlertDialog.dismiss();

      super.onDestroy();
      log("onDestroy()");
   }

// *********************************************************************************************************************
// *** LogCat a warn message if in debug mode                                                                        ***
// *********************************************************************************************************************

	void log(String msg) {
   	if (twigDebug)
      	Log.w(context.getString(R.string.app_name), context.getString(R.string.app_name) + " " + msg);
	}

}
