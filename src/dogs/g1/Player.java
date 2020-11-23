package dogs.g1;

import java.util.*;
import java.lang.Math;

import dogs.sim.*;
import dogs.sim.Directive.Instruction;


public class Player extends dogs.sim.Player {
    private final Double MAX_THROW_DIST = 40.0;
    List<ParkLocation> path;
    private Owner passTo;
    private ParkLocation passToLoc;
	
    /**
     * Player constructor
     *
     * @param rounds           number of rounds
     * @param numDogsPerOwner  number of dogs per owner
     * @param numOwners	       number of owners
     * @param seed             random seed
     * @param simPrinter       simulation printer
     *
     */
     public Player(Integer rounds, Integer numDogsPerOwner, Integer numOwners, Integer seed, Random random, SimPrinter simPrinter) {
         super(rounds, numDogsPerOwner, numOwners, seed, random, simPrinter);
         this.path = new ArrayList<>();
         this.passTo = null;
         // TEMP: temp workaround for owner.getLocation() bug
         this.passToLoc = null;
     }

    /**
     * Choose command/directive for next round
     *
     * @param round        current round
     * @param myOwner      my owner
     * @param otherOwners  all other owners in the park
     * @return             a directive for the owner's next move
     *
     */
    public Directive chooseDirective(Integer round, Owner myOwner, List<Owner> otherOwners) {
        simPrinter.println(myOwner.getNameAsString() + ": " + myOwner.getLocation().toString());
        Directive directive = new Directive();
        if (round == 1) {
            ParkLocation myLoc = getStartingLocation(myOwner, otherOwners);
            this.path = shortestPath(myLoc);
            this.passTo = getOwnerToPassTo(myOwner, otherOwners);
            simPrinter.print(myOwner.getNameAsString() + ": ");
            simPrinter.print("Loc: " + myLoc.toString() + ", ");
            simPrinter.println("PassTo: " + passTo.getNameAsString());
        }
        int roundWithAction = (round-1)/5;
        if (roundWithAction < this.path.size()) {
            directive.instruction = Instruction.MOVE;
            directive.parkLocation = this.path.get(roundWithAction);
            // maybe add a message saying we're still moving into position?
            return directive;
        }

        return throwBall(myOwner, otherOwners);
    }

    /**
     * Get the location where the current player will move to in the circle
     *
     * @param myOwner      my owner
     * @param otherOwners  all other owners in the park
     * @return             park location to which the player will move in circle
     *
     */
    public ParkLocation getStartingLocation(Owner myOwner, List<Owner> otherOwners) {
    	List<String> ownerNames = new ArrayList<>();
        String myName = myOwner.getNameAsString();
        ownerNames.add(myName);
        for (Owner owner : otherOwners)
            ownerNames.add(owner.getNameAsString());

        int numOwners = ownerNames.size();
        double dist = 40.0;     // use 40 for now
        List<ParkLocation> optimalStartingLocations = getOptimalLocationShape(numOwners, dist);
   
        Collections.sort(ownerNames);
        int myIndex = ownerNames.indexOf(myName);
        ParkLocation myLoc = optimalStartingLocations.get(myIndex);
        // TEMP: temp workaround for getLocations bug
        if (myIndex == 0) {
            this.passToLoc = optimalStartingLocations.get(optimalStartingLocations.size() -1);
        }
        else {
            this.passToLoc = optimalStartingLocations.get(myIndex - 1);
        }
        return myLoc;
    }

    /**
     * Get the optimal shape located closest to the park gates
     *
     * @param n            number of players
     * @param dist         distance between each player
     * @return             list of park locations where each player should go
     *
     */
    public List<ParkLocation> getOptimalLocationShape(Integer n, Double dist) {
        List<ParkLocation> shape = new ArrayList<ParkLocation>();
        if (n == 1)
            shape.add(new ParkLocation(0.0, 0.0));
        else if (n == 2) {
            double radian = Math.toRadians(45.0);
            shape.add(new ParkLocation(Math.cos(radian)*dist, 0.0));
            shape.add(new ParkLocation(0.0, Math.cos(radian)*dist));
        }
        else if (n == 3) {
            double radian1 = Math.toRadians(-15.0);
            double radian2 = Math.toRadians(-75.0);
            shape.add(new ParkLocation(0.0, 0.0));
            shape.add(new ParkLocation(Math.cos(radian1)*dist, -Math.sin(radian1)*dist));
            shape.add(new ParkLocation(Math.cos(radian2)*dist, -Math.sin(radian2)*dist));
        }
        else if (n == 4) {
            shape.add(new ParkLocation(0.0,0.0));
            shape.add(new ParkLocation(dist,0.0));
            shape.add(new ParkLocation(dist,dist));
            shape.add(new ParkLocation(0.0,dist));
        }
        else {
            double radianStep = Math.toRadians(360.0/n);
            double center = (dist/2)/(Math.sin(radianStep/2));
            double radius = center;
            double radian = Math.toRadians(135.0);
            for (int i = 0; i < n; i++) {
                double x = Math.cos(radian) * radius + center;
                double y = Math.sin(radian) * radius + center;
                shape.add(new ParkLocation(x,y));
                radian -= radianStep;
            }
        }
        return shape;
    }

    /**
     * Get the shortest path to starting point along which player will move
     *
     * @param start        starting point
     * @return             list of park locations along which owner will move to get to starting point
     *
     */
    public List<ParkLocation> shortestPath(ParkLocation start) {
        List<ParkLocation> path = new ArrayList<>();
        double magnitude = euclideanDistance(start.getRow(), start.getColumn());
        if (magnitude == 0)
            return path;
        
        double xStep = start.getRow()/magnitude;
        double yStep = start.getColumn()/magnitude;
        double xTemp = xStep*5;
        double yTemp = yStep*5;
        while (xTemp <= start.getRow() && yTemp <= start.getColumn()) {
            path.add(new ParkLocation(xTemp, yTemp));
            xTemp += xStep*5;
            yTemp += yStep*5;
        }
        path.add(start);
        return path;
    }

    public double euclideanDistance(double x, double y) {
        return Math.sqrt(Math.pow(x,2)+Math.pow(y,2));
    }

    public Double distanceBetweenTwoPoints(ParkLocation p1, ParkLocation p2) {
        Double x1 = p1.getColumn();
        Double y1 = p1.getRow();
        Double x2 = p2.getColumn();
        Double y2 = p2.getRow();

        return Math.sqrt(Math.pow(x1-x2,2)+Math.pow(y1-y2,2));
    }

    private Directive throwBall(Owner myOwner, List<Owner> otherOwners) {
        Directive directive = new Directive();
        directive.instruction = Instruction.NOTHING;

        List<Dog> waitingDogs = getAllWaitingDogs(myOwner, otherOwners);
        simPrinter.println(myOwner.getNameAsString() + ": " + waitingDogs.size());
        if (waitingDogs.size() > 0) {
            directive.dogToPlayWith = waitingDogs.get(0);
            directive.instruction = Instruction.THROW_BALL;
            // TEMP:
            directive.parkLocation = passToLoc;//passTo.getLocation();
        }

        simPrinter.print(myOwner.getNameAsString() + " is throwing to ");
        simPrinter.print(passTo.getNameAsString() + " at ");
        simPrinter.println(passToLoc);
        return directive;
    }

    // TEMP: owner.getLocation kept returning (0,0) so made this as a temp workaround
    private ParkLocation getOwnerLocation(Owner owner, List<Owner> owners) {
        Owner.OwnerName ownerName = owner.getNameAsEnum();
        for (Owner o: owners) {
            if (o.getNameAsEnum() == ownerName) {
                return o.getLocation();
            }
        }
        simPrinter.println("COULDN'T FIND MATCH");
        return owner.getLocation();
    }

    private List<Dog> getAllWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
        List<Dog> allWaitingDogs = new ArrayList<>();

        for (Dog dog: myOwner.getDogs()) {
            if (dog.isWaitingForOwner(myOwner)) {
                allWaitingDogs.add(dog);
            }
        }

        for (Owner otherOwner: otherOwners) {
            for (Dog dog: otherOwner.getDogs()) {
                if (dog.isWaitingForOwner(myOwner)) {
                    allWaitingDogs.add(dog);
                }
            }
        }
        return allWaitingDogs;
    }

    private Owner getOwnerToPassTo(Owner myOwner, List<Owner> otherOwners) {
        // filter out owners further than 40m
        /*otherOwners.removeIf(otherOwner ->
                distanceBetweenTwoPoints(owner.getLocation(), otherOwner.getLocation()) > MAX_THROW_DIST);
        return new Owner();*/
        List<String> ownerNames = new ArrayList<>();
        Map<String, Owner> ownersMap = new HashMap<>();
        String myName = myOwner.getNameAsString();
        ownerNames.add(myName);

        for (Owner owner: otherOwners) {
            ownerNames.add(owner.getNameAsString());
            ownersMap.put(owner.getNameAsString(), owner);
        }

        Collections.sort(ownerNames);
        int myIndex = ownerNames.indexOf(myName);
        if (myIndex == 0) {
            return ownersMap.get(ownerNames.get(ownerNames.size() - 1));
        }
        return ownersMap.get(ownerNames.get(myIndex - 1));
    }

    // Testing - run with "java dogs/g1/Player.java" in src folder
    public static void main(String[] args) {
        Random random = new Random();
        SimPrinter simPrinter = new SimPrinter(true);
        Player player = new Player(1, 1, 1, 1, random, simPrinter);

        // TEST 1 - optimal line
        double dist = 2*Math.sqrt(2);
        int n = 2;
        List<ParkLocation> optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);

        // TEST 2 - optimal equilateral triangle
        double radian = Math.toRadians(-15);
        dist = Math.cos(radian)*5;
        n = 3;
        optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);

        // TEST 3 - optimal square
        dist = 2;
        n = 4;
        optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);

        // TEST 4 - optimal regular pentagon
        dist = 3;
        n = 5;
        optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);

        // TEST 5 - optimal regular hexagon
        dist = 5;
        n = 6;
        optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);

        // TEST 6 - optimal regular octagon
        dist = Math.sqrt(10);
        n = 8;
        optimalShape = player.getOptimalLocationShape(n, dist);
        System.out.println(optimalShape);
    }
}