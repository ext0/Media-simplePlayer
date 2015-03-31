package simplePlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.Terminal.Color;

import javafx.embed.swing.JFXPanel;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;

public class Main implements Runnable{
	public static File musicPath=new File("C:\\Users\\Titanic\\Music");
	static Terminal terminal = TerminalFacade.createSwingTerminal();
	static Screen screen = new Screen(terminal);
	static ArrayList<MediaPlayer>songs=new ArrayList<MediaPlayer>();
	static ArrayList<File>directories=new ArrayList<File>();
	static MediaPlayer current;
	static Thread t;
	static Thread r;
	static int preferredFrames=30;
	static int currentSongIndex=0;
	static int terminalX=0;
	static int terminalY=0;
	static int songSliderIndex=0;
	static int balance=0;
	static int numberOfBands=32;
	static int resetTime=20;
	static int maxWavelength=30;
	static int genreSliderIndex=0;
	static int currentFrames=0;
	static double updateInterval=0.03;
	static int track=0;
	static double volume=30;
	static long startTime;
	static long endTime;
	static boolean play=true;
	static boolean dataReady=true;
	static boolean jumpForward=false;
	static boolean jumpBackward=false;
	static boolean shuffle=true;
	static boolean isPickingGenre=true;
	static boolean initialized=false;
	static String genre=null;
	static String selectedGenre;
	static Color spectrumColor=Color.CYAN;
	static ArrayList<Float>bandValues=new ArrayList<Float>();
	static AudioSpectrumListener spectrumListener;
	public static void main(String[]args) throws InterruptedException{
		File[]dir=musicPath.listFiles();
		for (File f:dir){
			if (f.isDirectory()) directories.add(f);
		}
		spectrumListener = new AudioSpectrumListener() {
			public void spectrumDataUpdate(double timestamp, double duration,float[] magnitudes, float[] phases) {
				for (int i = 0; i < magnitudes.length-1; i++) {
					bandValues.set(i,magnitudes[i] + 60); //+60 for correction, thanks javaFX
					dataReady=true;
					//screen.getTerminal().clearScreen();
					//drawSpectrumHorizontal(85,terminal.getTerminalSize().getRows()+2,1,2);
				}
			}
		};
		screen.startScreen();
		terminal.setCursorVisible(false);
		r=new Thread(new Runnable(){ 
			public void run(){
				genreSliderIndex=0;
				selectedGenre=directories.get(0).getName();
				Key k=null;
				while (isPickingGenre){
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					terminal.clearScreen();
					//terminal.putCharacter('x');
					//nextLine(terminalX);
					drawGenreChoose(0,0);	
					drawLogo(10,10,true);
					if ((k=terminal.readInput())!=null){
						System.out.println(k.getKind());
						if (k.getKind().toString().equals("ArrowDown")){
							System.out.println("ArrowDown");
							if (genreSliderIndex==directories.size()-1){
								genreSliderIndex=0;
								selectedGenre=directories.get(genreSliderIndex).getName();
							}
							else{
								genreSliderIndex++;
								selectedGenre=directories.get(genreSliderIndex).getName();
							}
						}
						else if (k.getKind().toString().equals("ArrowUp")){
							if (genreSliderIndex==0){
								genreSliderIndex=directories.size()-1;
								selectedGenre=directories.get(genreSliderIndex).getName();
							}
							else{
								genreSliderIndex--;
								selectedGenre=directories.get(genreSliderIndex).getName();
							}
						}
						else if (k.getKind().toString().equals("Enter")){
							selectedGenre=directories.get(genreSliderIndex).getName();
							isPickingGenre=false;
							return;
						}
					}
				}
			}
		});
		r.start();
		while (isPickingGenre){
			Thread.sleep(100);
		}
		Thread.sleep(100);
		System.out.println("ayy");
		startToolkit();
		if (isPickingGenre){

		}
		getSongs();
		if (shuffle){
			int dice=randInt(0,songs.size()-1);
			currentSongIndex=dice;
		}
		current=songs.get(currentSongIndex);
		t =new Thread(new Main());
		t.start();
		current.play();
		updateBandStorage();
		current.setVolume(volume/100);
		updateRunnable();
		current.setAudioSpectrumListener(spectrumListener);
		current.setAudioSpectrumInterval(updateInterval);
		current.setAudioSpectrumNumBands(numberOfBands);
		//screen.startScreen();
		//terminal.setCursorVisible(false);
		initialized=true;
		for (;;){
			if (isPickingGenre){
				
			}
			else{
				Thread.sleep((long)(updateInterval*1000));
				if (dataReady){ 
					//THIS WHOLE LOOP IS
					//PERFECTLY SYNCED
					//DO NOT TOUCH
					screen.getTerminal().clearScreen();
					drawScreen();
					dataReady=false;
				}
				
				if ((current.getStatus().equals(Status.PAUSED))&&(play)){
					current.play();
				}
				else if ((current.getStatus().equals(Status.PLAYING))&&(!play)){
					current.pause();
				}
			}
		}
	}
	public static void drawScreen(){
		drawSongList(0,0,songSliderIndex);
		drawCurrentSong(50,8);
		drawControls(57,25);
		drawProgressBar(50,20,30);
		drawLogo(90,10,false);
		drawProgressBar(50,20,30);
		drawSpectrumHorizontal(85,terminal.getTerminalSize().getRows()+2,1,2);
	}
	public void run(){ //multithreaded goodness
		Key input;
		for (;;){
			input=terminal.readInput();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if (input!=null){
				if (input.getKind().toString().equals("PageDown")){
					if (songSliderIndex<songs.size()-(terminal.getTerminalSize().getRows()-1))
						songSliderIndex++;
				}
				else if (input.getKind().toString().equals("PageUp")){
					if (songSliderIndex!=0){
						songSliderIndex--;
					}
				}
				else if (input.getCharacter()==' '){
					if (play) play=false;
					else play=true;
				}
				else if (input.getCharacter()=='.'){
					if (volume<100){
						volume+=10;
						current.setVolume(volume/100);
					}
				}
				else if (input.getCharacter()==','){
					if (volume>0){
						volume-=10;
						current.setVolume(volume/100);
					}
				}
				else if (input.getCharacter()=='['){
					if (balance>-10){
						balance--;
						current.setBalance(((double)balance)/10.0);
					}
				}
				else if (input.getCharacter()==']'){
					if (balance<10){
						balance++;
						current.setBalance(((double)balance)/10.0);
					}
				}
				else if (input.getCharacter()=='>'){
					current.setRate(current.getRate()+0.1);
				}
				else if (input.getCharacter()=='<'){
					current.setRate(current.getRate()-0.1);
				}
				else if (input.getKind().toString().equals("ArrowDown")){
					if (currentSongIndex+1>songs.size()-1){
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						currentSongIndex=0;
						songSliderIndex=0;
						changeSong(currentSongIndex);
					}
					else{
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						currentSongIndex++;
						changeSong(currentSongIndex);
					}
				}
				else if (input.getKind().toString().equals("ArrowUp")){
					if (currentSongIndex-1<0){
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						currentSongIndex=songs.size()-1;
						changeSong(currentSongIndex);
						songSliderIndex=(songs.size()-1)-(terminal.getTerminalSize().getRows()-2);
					}
					else{
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						currentSongIndex--;
						changeSong(currentSongIndex);
					}
				}
				else if (input.getCharacter()=='s'){
					if (shuffle) shuffle=false;
					else shuffle=true;
				}
				else if (input.getKind().toString().equals("Escape")){
					System.exit(-1);
				}
			}
		}
	}

	public static MediaPlayer getMediaPlayer(String s){
		return new MediaPlayer(new Media(new File(directories.get(genreSliderIndex).toString()+"\\"+s).toURI().toString()));
	}

	public static void drawCycles(int x,int y){
		terminal.moveCursor(x, y);
		terminalX=x;
		terminalY=y;
		writeString(String.format("%d/%d [Running at %d%%]", currentFrames,preferredFrames,(currentFrames/preferredFrames)*100));
	}
	public static void drawSongList(int x,int y,int i){
		terminal.moveCursor(x, y);
		terminalX=x;
		terminalY=y;
		int check=0;
		writeString(String.format("    Songs [%02d/%02d]    ",i,songs.size()));
		for (int iterator=i;iterator<songs.size();iterator++){
			check++;
			if (check>terminal.getTerminalSize().getRows()-1) break;
			nextLine(x);
			if (songs.get(iterator).equals(current)){
				terminal.applyForegroundColor(Color.CYAN);
				writeString(String.format("%s", parseSource(songs.get(iterator).getMedia().getSource())));
				terminal.applyForegroundColor(Color.DEFAULT);
			}
			else
				writeString(String.format("%s", parseSource(songs.get(iterator).getMedia().getSource())));
		}
		//System.out.printf("Total of %d songs printed (TermSize:%d, SongSelection: %d)\n",Math.min(terminal.getTerminalSize().getRows(), songs.size())-i,terminal.getTerminalSize().getRows(),songs.size());
	}
	public static void drawGenreChoose(int x,int y){
		terminal.moveCursor(x, y);
		terminalX=x;
		terminalY=y;
		writeString("     Genres    ");
		nextLine(x);
		for (File f:directories){
			if (selectedGenre.equals(f.getName())){
				terminal.applyForegroundColor(Color.CYAN);
				writeString(f.getName());
				terminal.applyForegroundColor(Color.DEFAULT);
			}
			else
				writeString(f.getName());
			nextLine(x);
		}

	}
	public static void drawProgressBar(int x,int y,int length){
		terminal.moveCursor(x, y);
		terminalX=x;
		terminalY=y;
		writeString("[");
		double currentTime=current.getCurrentTime().toMillis();
		double totalTime=current.getMedia().getDuration().toMillis();
		int toMove=(int)((currentTime/totalTime)*length);
		for (int i=0;i<length;i++){
			if (toMove>0){
				writeString("_");
				toMove--;
			}
			else if (toMove==0){
				writeString(">");
				toMove--;
			}
			else if (toMove<0){
				writeString("_");
				toMove--;
			}
		}
		writeString("]");
	}
	public static void drawCurrentSong(int x,int y){
		terminal.moveCursor(x, y);
		terminalX=x;
		terminalY=y;
		writeString("  Current Song  ");
		nextLine(x);
		writeString("Title:  "+parseTitle(songs.get(currentSongIndex)));
		nextLine(x);
		writeString("Artist: "+parseArtist(songs.get(currentSongIndex)));
		nextLine(x);
		nextLine(x);
		writeString("Volume    [");
		double placehold=0;
		while (placehold<volume-0.01){ //bad practice, fix later
			placehold+=10;
			writeString("|");
		}
		placehold=100;
		while (placehold>volume){
			writeString(" ");
			placehold-=10;
		}
		writeString("]");
		nextLine(x);
		writeString("Balance   ["+current.getBalance()+"]");
		nextLine(x);
		writeString("Current   ["+String.format("%02dm:%02ds", 
				TimeUnit.MILLISECONDS.toMinutes((long)current.getCurrentTime().toMillis()),
				TimeUnit.MILLISECONDS.toSeconds((long)current.getCurrentTime().toMillis()) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)current.getCurrentTime().toMillis()))
				)+"]");
		nextLine(x);
		writeString("Duration  ["+String.format("%02dm:%02ds", 
				TimeUnit.MILLISECONDS.toMinutes((long)current.getMedia().getDuration().toMillis()),
				TimeUnit.MILLISECONDS.toSeconds((long)current.getMedia().getDuration().toMillis()) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)current.getMedia().getDuration().toMillis()))
				)+"]");
		nextLine(x);
		writeString("Speed     ["+current.getRate()+"x]");
		nextLine(x);
		if (shuffle)
			writeString("Shuffle   [ENABLED]");
		else
			writeString("Shuffle   [DISABLED]");
	}

	public static void drawControls(int x,int y){
		terminal.moveCursor(x, y);
		terminalX=x;
		terminalY=y;
		if (play){
			writeString(" <  (PLAYING)  >");
		}
		else{
			writeString(" <  (PAUSED)  >");
		}
		nextLine(x);
	}
	public static void drawSpectrumHorizontal(int x,int y, int i,int a){
		terminal.moveCursor(x, y);
		terminal.applyBackgroundColor(spectrumColor);
		terminalX=x;
		terminalY=y;
		int tracker;
		int toMove;
		for (tracker=0;tracker<numberOfBands/i;tracker++){
			if (bandValues.size()-1<tracker){
				break;
			}
			toMove=Math.round(bandValues.get(tracker))/2;
			if (toMove==0){

			}
			else if (toMove>0){
				for (int h=0;h<a;h++){
					for (int q=0;q<toMove;q++){
						terminalY--;
						if ((q>5)&&(q<10))
							terminal.applyBackgroundColor(Color.BLUE);
						else if (q>9){
							terminal.applyBackgroundColor(Color.MAGENTA);
						}
						terminal.moveCursor(terminalX,terminalY);
						terminal.putCharacter(' ');
						terminal.applyBackgroundColor(spectrumColor);
					}
					terminalX++;
					terminalY=y;
					terminal.moveCursor(terminalX, y);
				}
			}
			else if(toMove<0){
				for (int q=0;q<Math.abs(toMove);q++){
					terminalY++;
					terminal.moveCursor(terminalX,terminalY);
					terminal.putCharacter(' ');
				}
			}
			terminalY=y;
			terminalX++;
			terminal.moveCursor(terminalX+1, terminalY);
		}
		terminal.applyBackgroundColor(Color.DEFAULT);
	}
	public static void drawSpectrumVertical(int x,int y, int i,int a){
		terminal.moveCursor(x, y);
		terminal.applyBackgroundColor(spectrumColor);
		terminalX=x;
		terminalY=y;
		int tracker;
		int toMove;
		for (tracker=0;tracker<numberOfBands/i;tracker++){
			if (bandValues.size()-1<tracker){
				break;
			}
			toMove=Math.round(bandValues.get(tracker))/2;
			if (toMove==0){

			}
			else if (toMove>0){
				for (int q=0;q<toMove;q++){
					terminalX--;
					terminal.moveCursor(terminalX,terminalY);
					terminal.putCharacter(' ');
				}
			}
			else if(toMove<0){
				for (int q=0;q<Math.abs(toMove);q++){
					terminalX++;
					terminal.moveCursor(terminalX,terminalY);
					terminal.putCharacter(' ');
				}
			}
			terminalY++;
			terminalX=x;
			terminal.moveCursor(terminalX, terminalY);
		}
		terminal.applyBackgroundColor(Color.DEFAULT);
	}
	public static void drawLogo(int x,int y,boolean credit){
		terminal.moveCursor(x, y);
		terminalX=x;
		terminalY=y;
		terminal.applyForegroundColor(Color.RED);
		writeString("      _                 _      _____  _                       ");    nextLine(x);
		terminal.applyForegroundColor(Color.YELLOW);
		writeString("     (_)               | |    |  __ \\| |                      ");   nextLine(x);
		terminal.applyForegroundColor(Color.GREEN);
		writeString("  ___ _ _ __ ___  _ __ | | ___| |__) | | __ _ _   _  ___ _ __ ");    nextLine(x);
		terminal.applyForegroundColor(Color.CYAN);
		writeString(" / __| | '_ ` _ \\| '_ \\| |/ _ \\  ___/| |/ _` | | | |/ _ \\ '__|");nextLine(x);
		terminal.applyForegroundColor(Color.BLUE);
		writeString(" \\__ \\ | | | | | | |_) | |  __/ |    | | (_| | |_| |  __/ |   ");  nextLine(x);
		writeString(" |___/_|_| |_| |_| .__/|_|\\___|_|    |_|\\__,_|\\__, |\\___|_|   ");nextLine(x);
		terminal.applyForegroundColor(Color.MAGENTA);
		writeString("                 | |                           __/ |          ");    nextLine(x);
		terminal.applyForegroundColor(Color.WHITE);
		writeString("                 |_|                          |___/           ");    nextLine(x);
		if (credit){
			writeString("                    Created by Patrick Bell          "); 	      nextLine(x);
		}
		terminal.applyForegroundColor(Color.DEFAULT);
		
	}
	public static void changeSong(int index){
		if (songs.get(index)==null)
			return;
		current.stop();
		currentSongIndex=index;
		current=songs.get(currentSongIndex);
		current.play();
		current.setVolume(volume/100);
		current.setBalance(balance);
		current.setAudioSpectrumListener(spectrumListener);
		current.setAudioSpectrumInterval(updateInterval);
		current.setAudioSpectrumNumBands(numberOfBands);
		updateBandStorage();
		updateRunnable();
		play=true;
	}
	public static void getSongs(){
		for (File f:directories.get(genreSliderIndex).listFiles()){
			if (f.getName().endsWith(".mp3")){
				songs.add(getMediaPlayer(f.getName()));
			}
		}
	}

	public static String parseSource(String s){
		return (s.substring(s.lastIndexOf('/')+1, s.length()-4)).replace("%20", " ");
	}

	public static String parseArtist(MediaPlayer s){
		String source=parseSource(s.getMedia().getSource());
		return source.substring(0, source.indexOf('-'));
	}

	public static String parseTitle(MediaPlayer s){
		String source=parseSource(s.getMedia().getSource());
		return source.substring(source.indexOf('-')+2, source.length());
	}
	public static void updateBandStorage(){
		bandValues.clear();
		for (int i=0;i<current.getAudioSpectrumNumBands();i++){
			bandValues.add(0.0F);
		}
	}
	public static void updateRunnable(){
		current.setOnEndOfMedia(new Runnable() {
			public void run() {
				if (shuffle){
					int dice=randInt(0,songs.size()-1);
					currentSongIndex=dice;
					changeSong(currentSongIndex);
				}
				else{
					if (currentSongIndex+1>songs.size()-1){
						currentSongIndex=0;
						songSliderIndex=0;
						changeSong(currentSongIndex);
					}
					else{
						currentSongIndex++;
						changeSong(currentSongIndex);
					}
				}
			}
		});
	}
	public static void writeString(String s){
		for (char c:s.toCharArray()){ 
			terminal.putCharacter(c); 
			terminalX++;
		}
	}
	public static void nextLine(int x){
		terminalX=x;
		terminalY++;
		terminal.moveCursor(terminalX, terminalY);
	}
	public static int randInt(int min, int max) {
		Random rand = new Random();
		int randomNum = rand.nextInt((max - min) + 1) + min;
		return randomNum;
	}

	public static boolean startToolkit(){
		final CountDownLatch latch = new CountDownLatch(1);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new JFXPanel(); // initializes JavaFX environment
				latch.countDown();
			}
		});
		try {
			latch.await();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
}
