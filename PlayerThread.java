/*
 * PlayerThread.java
 */


import java.util.Random;
import java.util.ArrayList;

public class PlayerThread implements Runnable {

    //fields
    protected String threadName;
    protected Thread t;
    protected boolean isWinner;
    protected boolean hasCarrot;
    protected Board board;
  

    //constructor 
    public PlayerThread(String name, Board referenceToBoard) {
        threadName = name;
        board = referenceToBoard;

    }

    //override run() method
    public void run() {
        
        while ( !isWinner ) {
            try {
                movePlayer();
                Thread.currentThread().sleep(1); //Important. Allows other threads to get lock. Else current thread will almost always re-lock the board.
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                System.out.println(threadName + " has been interrupted!");
                break;
            } catch (Exception e) {
                System.out.print(e.toString() + ". A weird exception happened.");
            }
        }
    }
    
    
    //moves player to random adjacent spot
    // We can make this more elegant later. For looping through the whole grid is silly.
    //Locks down board do that it can't change while this is being run.
    private void movePlayer() throws InterruptedException {
        synchronized (board) {
            
            if ( board.getIsThereAWinner() ) {
                throw new InterruptedException ("Game over");
            }
            
            //get current location
            
            int[] currentLocation = new int[] { -1, -1};
            
            for (int i = 0; i < board.getROWS(); i++) {
                for (int j = 0; j < board.getCOLUMNS(); j++) {
                    if ( board.board[i][j].isThisPlayerHere( threadName ) ) {
                        currentLocation[0] = i;
                        currentLocation[1] = j;
                    }
                }
            }
            
            if ( currentLocation[0] == -1 ) {
                throw new InterruptedException (threadName + " is still dead!");
            }
            
            System.out.println(threadName + " is currently at:" + currentLocation[0] + "," + currentLocation[1]);
            
            ArrayList<int[]> possibleMoves = new ArrayList<int[]>();
            
            //                               row             col                       hasCarrot
            if ( board.canPlayerMoveHere(currentLocation[0] - 1, currentLocation[1], hasCarrot) ) { //up
                possibleMoves.add( new int[] { currentLocation[0] - 1, currentLocation[1]} );
            }
            if ( board.canPlayerMoveHere(currentLocation[0] + 1, currentLocation[1], hasCarrot) ) { //down
                possibleMoves.add( new int[] { currentLocation[0] + 1, currentLocation[1]} );
            }
            if ( board.canPlayerMoveHere(currentLocation[0], currentLocation[1] - 1, hasCarrot) ) { //left
                possibleMoves.add( new int[] { currentLocation[0], currentLocation[1] - 1} );
            }
            if ( board.canPlayerMoveHere(currentLocation[0], currentLocation[1] + 1, hasCarrot) ) { //right
                possibleMoves.add( new int[] { currentLocation[0], currentLocation[1] + 1} );
            }
            
            //Check how many moves can be made
            int numPossibleMoves = possibleMoves.size();
            
            if ( numPossibleMoves == 0 ) {
                System.out.println(threadName + " cannot move!  This message is from PlayerThread.movePlayer() ");
                return; //exits this method.
            }
            
            Random randomObject = new Random();
            
            int wayToPick = randomObject.nextInt( numPossibleMoves );
            
            moveThisPlayerToThatSpace( currentLocation, possibleMoves.get(wayToPick));
            
            System.out.println(threadName + ": Has moved!");
            System.out.println("From: " + currentLocation[0] + "," + currentLocation[1] + "   to: " + possibleMoves.get(wayToPick)[0] + "," + possibleMoves.get(wayToPick)[1]);
            board.printBoard();
            
            //increase counter for mt
            Board.mtCounter++;
            System.out.println ("increasing counter "+ Board.mtCounter);
            
            
            if((Board.mtCounter) %3==0 ){
                System.out.println("Mountain needs to move");
                updateMountain();
                board.mtLocation();
                
            }
            
            
            //press enter to continue
            System.out.println("Press enter to continue");
            try{
                System.in.read();
            }
            catch(Exception e) {
                //do nothing
            }
            System.out.println("Press enter to continue");
            
            //if on carrot, set hasCarot!
            if ( isPlayerOnCarrot( possibleMoves.get(wayToPick)[0], possibleMoves.get(wayToPick)[1] ) ) {
                setHasCarrot();
            }
            
            //if have carrot and on mountain, set this player as winner
            System.out.println("Checking if "+threadName+ " is the winner...");
            if ( hasCarrot && isPlayerOnMountain( possibleMoves.get(wayToPick)[0], possibleMoves.get(wayToPick)[1] ) ) {
                
                System.out.println(threadName + " is the winner! WOW!");
                setPlayerAsWinner();
                board.setWinner(threadName);
            }
        }
    }
    
    
    public void updateMountain(){
        Random position= new Random();
        int row = position.nextInt( board.getROWS() );
        int column = position.nextInt( board.getCOLUMNS() );
                
        while (board.board[row][column].isThisSpaceOccupiedByAnything()){

            row = position.nextInt( board.getROWS() );
            column = position.nextInt( board.getCOLUMNS() );
        }
        
        System.out.println("Updating mt");
        board.removeMt();
        
        board.board[row][column].setOccupant("Mountain");
        board.newMt(row,column);
        
        board.printBoard();
        
    }
    
    protected boolean isPlayerOnMountain(int row, int col) {
        if ( board.board[row][col].getIsMountainHere() ) {
            
            return true;
        }
        
        return false;
    }
    
    
    protected boolean isPlayerOnCarrot( int row, int col ) {
        if ( board.board[row][col].getIsCarrotHere() ) {
            
            return true;
        }

        return false;
    }
    
    
    //Method that handles moving players around
    protected void moveThisPlayerToThatSpace(int[] currentSpace, int[] wayToGo) {
        //  If have carrot, also move carrot.
        if ( hasCarrot ) {
            board.board[ currentSpace[0] ][ currentSpace[1] ].removeOccupant( "Carrot" );
            board.board[ wayToGo[0] ][ wayToGo[1] ].setOccupant( "Carrot" );
        }
        
        //set player as being in new space
        board.board[ wayToGo[0] ][ wayToGo[1] ].setOccupant( threadName );
        
        //remove player from old space
        board.board[ currentSpace[0] ][ currentSpace[1] ].removeOccupant( threadName );
    }
    

    protected void setPlayerAsWinner() {
        isWinner = true;
        board.setWinner( threadName );
        board.setIsThereAWinner(true);

    }


    public boolean getIsWinner() {
        return isWinner;
    }


    public void setHasCarrot() {
        hasCarrot = true;
    }

}
