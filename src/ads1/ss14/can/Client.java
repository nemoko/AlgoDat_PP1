package ads1.ss14.can;

import ads1.ss14.can.exceptions.CANException;
import ads1.ss14.can.exceptions.NoAdditionalStorageAvailable;
import ads1.ss14.can.exceptions.NoSuchDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import static java.lang.Math.nextAfter;
import static java.lang.Math.sqrt;

public class Client implements ClientInterface, ClientCommandInterface{

    private String uniqueID;
    private int networkXSize;
    private int getNetworkYSize;
    private int maxNumberOfDocuments = -1;
    private HashMap<String,Pair<Document,Position>> library = new HashMap<String, Pair<Document, Position>>();
    private Position position;
    private Area area;
    private ArrayList<ClientInterface> neighbourList = new ArrayList<ClientInterface>();

    /**
     * Constructs a new Client
     * @param uniqueID the ID of the Client in the CAN
     * @param networkXSize the size along the horizontal axis in the CAN
     * @param networkYSize the size along the vertical axis in the CAN
     */
    public Client(String uniqueID, int networkXSize, int networkYSize) {
        this.uniqueID = uniqueID;
        this.networkXSize = networkXSize;
        this.getNetworkYSize = networkYSize;

        setArea(new Area(0, networkXSize, 0, networkYSize));
    }

    @Override
    public String getUniqueID() {
        return uniqueID;
    }

    @Override
    public void setMaxNumberOfDocuments(int m) {
        maxNumberOfDocuments = m;
    }

    @Override
    public int getMaxNumberOfDocuments() {
        return maxNumberOfDocuments;
    }

    @Override
    public Document getDocument(String documentName) throws NoSuchDocument {
        //TODO fixed?
        /*
        * Diese Funktion darf nur fuer Clients aufgerufen werden, die das gesuchte Dokument auch tatsaechlich speichern koennten.
        * Der Aufruf dieser Funktion bei anderen Clients kann vom Abgabesystem als Fehler bewertet werden!
        * Sie koennen daher nicht einfach mit Brute Force alle Clients nach einem Dokument durchsuchen.
        * Sie muessen den Algorithmus wie in Abschnitt Funtionsweise beschrieben implementieren,
        * ansonsten kann Ihre Abgabe beim Abgabegespraech negativ beurteilt werden!
        * */


        //calculate document position using hash, if not owned by this or neighbours, route request ?
        //if(library.size() < maxNumberOfDocuments)
        if(library.containsKey(documentName))
            return library.get(documentName).first;


        throw new NoSuchDocument();
    }

    @Override
    public void storeDocument(Document d, Position p) throws NoAdditionalStorageAvailable, CANException {
        Pair<Document,Position> newDoc = new Pair<Document, Position>(d,p);

        if(library.size() < maxNumberOfDocuments)
            library.put(d.getName(), newDoc);
        else
            throw new NoAdditionalStorageAvailable();
    }

    @Override
    public void deleteDocument(String documentName) throws NoSuchDocument {
        if(library.containsKey(documentName))
            library.remove(documentName);

        throw new NoSuchDocument();
    }

    @Override
    public Position getPosition() {
        return position;
    }

    public void setPosition(double lowerX, double upperX, double lowerY, double upperY) {
        position = new Position((upperX-lowerX)/2,(upperY-lowerY)/2); //TODO not according to the assignement
    }

    @Override
    public Area getArea() {
        return area;
    }

    @Override
    public void setArea(Area newArea) {
        area = newArea;
        setPosition(area.getLowerX(),area.getUpperX(),area.getLowerY(),area.getUpperY());
    }

    @Override
    public Iterable<ClientInterface> getNeighbours() {
        return neighbourList;
    }

    @Override
    public void addNeighbour(ClientInterface newNeighbour){
        this.neighbourList.add(newNeighbour);
    }

    @Override
    public void removeNeighbour(String clientID) {
        for(int i = 0; i < neighbourList.size(); i++) {
            Client client = (Client) neighbourList.get(i);

            if( client.getUniqueID().equals(clientID)) {
                neighbourList.remove(i);
                return;
            }
        }
    }

    @Override //STUFE 1
    public ClientInterface searchForResponsibleClient(Position p) {
        if(this.getArea().contains(p)) {
            return this;
        }

        //search all neighbors
        for(ClientInterface n : neighbourList) {
            if(n.getArea().contains(p))
                return n;
        }

        //find closest path to P via closest neighbor to P
        double closestDist = Double.MAX_VALUE;
        Pair<ClientInterface,Double> closestNeighbour = new Pair<ClientInterface, Double>(this,closestDist);

        int index = 0;
        for(int i = 0; i < neighbourList.size(); i++) {
            ClientInterface cv = neighbourList.get(i);

            double rX = max(min(cv.getArea().getUpperX(),p.getX()),cv.getArea().getLowerX());
            double rY = max(min(cv.getArea().getUpperY(),p.getY()),cv.getArea().getLowerY());
            double neighbourDistance = sqrt( (rX - p.getX())*(rX - p.getX()) + (rY - p.getY())*(rY - p.getY()) ); //Q

            Pair<ClientInterface,Double> neighbour = new Pair<ClientInterface, Double>(cv,neighbourDistance);

            if(closestNeighbour.second < neighbour.second) { //compare distances
                //do nothing
            }
            else if(closestNeighbour.second == neighbour.second) //compare names
            {
                closestNeighbour = lexiCompare(closestNeighbour,neighbour);
                index = i;
            }
            else
            {
                closestNeighbour = neighbour;
                index = i;
            }
        }
        //retrieve the client with the shortest distance
        return neighbourList.get(index).searchForResponsibleClient(p);
    }

    @Override
    public ClientInterface joinNetwork(ClientInterface entryPoint, Position p) throws CANException {

        //if this is the first client in the network
        if(entryPoint == null)
            return this;

        //if this client is responsible for P
        if(this.getPosition().equals(p)) {
            Area entry = entryPoint.getArea();
            Pair<Area, Area> pair;

        /* SPLITTING AREA */
            if(entry.getUpperY()-entry.getLowerY() > entry.getUpperX() - entry.getLowerX())
            {
                pair = entry.splitHorizontally();
                this.setArea(pair.second);
                entryPoint.setArea(pair.first);
            }
            else
            {
                pair = entry.splitVertically();
                this.setArea(pair.first);
                entryPoint.setArea(pair.second);
            }
        /* END of SPLIT */

            adaptNeighbours(entryPoint);
            //TODO REASSIGN DOCS

        }
        else // if this client isnt responsible for P
        {
            return joinNetwork(this.searchForResponsibleClient(p),p); //TODO i dont even know anymore
        }
        return entryPoint;
    }

    @Override
    public Iterable<Pair<Document, Position>> removeUnmanagedDocuments() {

        LinkedList<Pair<Document,Position>> removeDocs = new LinkedList<Pair<Document, Position>>();

        for(String doc : library.keySet()) {
            if(!getArea().contains(library.get(doc).second))
                removeDocs.add(library.get(doc));
        }
        return removeDocs;
    }

    @Override
    public void adaptNeighbours(ClientInterface joiningClient) {

        //and this as a neighbour to the joining client, and the other way around
        this.addNeighbour(joiningClient);
        joiningClient.addNeighbour(this);

        /***********************
         *          |           *
         *          | {joining} *
         *          | {client}  *
         *          |           *
         *          |           *
         ************************/
        //vertical split
        if(this.getArea().getUpperX() == joiningClient.getArea().getLowerX()) {

            for(ClientInterface neighbor : neighbourList)
            {
                //upper neighbors
                if (joiningClient.getArea().getUpperY() == neighbor.getArea().getLowerY()) {

                    //neighbor belongs only to the joining client
                    if(joiningClient.getArea().getLowerX() < neighbor.getArea().getLowerX()) {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                        neighbor.removeNeighbour(this.getUniqueID());
                        this.removeNeighbour(neighbor.getUniqueID());
                        continue;
                    }

                    //both share the neighbor
                    if(joiningClient.getArea().getLowerX() > neighbor.getArea().getLowerX() &&
                            joiningClient.getArea().getLowerX() < neighbor.getArea().getUpperX())
                    {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                    }
                }

                //bottom neighbors
                if(joiningClient.getArea().getLowerY() == neighbor.getArea().getUpperY()) {

                    //neighbor belongs only to the joining client
                    if(joiningClient.getArea().getLowerX() < neighbor.getArea().getLowerX()) {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                        neighbor.removeNeighbour(this.getUniqueID());
                        this.removeNeighbour(neighbor.getUniqueID());
                        continue;
                    }

                    //both share the neighbor
                    if(joiningClient.getArea().getLowerX() > neighbor.getArea().getLowerX() &&
                            joiningClient.getArea().getLowerX() < neighbor.getArea().getUpperX())
                    {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                    }
                }

                //right side
                if(joiningClient.getArea().getUpperX() == neighbor.getArea().getLowerX()) {
                    joiningClient.addNeighbour(neighbor);
                    neighbor.addNeighbour(joiningClient);
                    this.removeNeighbour(neighbor.getUniqueID());
                    neighbor.removeNeighbour(this.getUniqueID());
                }
            }
            return;
        }

        /*********************
         *                    *
         *                    *
         *--------------------*
         *  {joining client}  *
         *                    *
         **********************/
        if(this.getArea().getLowerY() == joiningClient.getArea().getUpperY()) {

            for(ClientInterface neighbor : neighbourList)
            {
                //left side
                if(joiningClient.getArea().getLowerX() == neighbor.getArea().getUpperX()) {


                    //neighbor belongs to joining client only
                    if(joiningClient.getArea().getUpperY() >= neighbor.getArea().getUpperY())
                    {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                        neighbor.removeNeighbour(this.getUniqueID());
                        this.removeNeighbour(neighbor.getUniqueID());
                        continue;
                    }

                    //share the neighbor
                    if(joiningClient.getArea().getUpperY() < neighbor.getArea().getUpperY() &&
                            joiningClient.getArea().getLowerY() > neighbor.getArea().getLowerY())
                    {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                    }
                }

                //right side
                if(joiningClient.getArea().getUpperX() == neighbor.getArea().getLowerX()) {

                    //neighbor belongs to the joining client only
                    if(joiningClient.getArea().getUpperY() >= neighbor.getArea().getUpperY()) {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                        neighbor.removeNeighbour(this.getUniqueID());
                        this.removeNeighbour(neighbor.getUniqueID());
                        continue;
                    }

                    //share the neighbor
                    if(joiningClient.getArea().getUpperY() < neighbor.getArea().getUpperY() &&
                            joiningClient.getArea().getUpperY() > neighbor.getArea().getLowerY())
                    {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                    }
                }

                //bottom side
                if(joiningClient.getArea().getLowerY() == neighbor.getArea().getUpperY()) {
                    //belongs to joining client only
                    joiningClient.addNeighbour(neighbor);
                    neighbor.addNeighbour(joiningClient);
                    neighbor.removeNeighbour(this.getUniqueID());
                    this.removeNeighbour(neighbor.getUniqueID());
                }
                //top nothing to do
            }
        }
    }

    @Override //STUFE 2
    public void addDocumentToNetwork(Document d) throws CANException {

        for(int i = 0; i < (networkXSize * getNetworkYSize); i++) {

            Position p = hashDocument(d.getName(),i);
            ClientInterface c = searchForResponsibleClient(p);

            try {
                c.storeDocument(d,p);
            }
            catch (NoAdditionalStorageAvailable e) {
                continue;
            }
        }
    }

    @Override //STUFE 2
    public void removeDocumentFromNetwork(String documentName) {

        for(int i = 0; i < (networkXSize * getNetworkYSize); i++) {

            Position p = hashDocument(documentName,i);
            ClientInterface c = searchForResponsibleClient(p);

            try {
                c.deleteDocument(documentName);
            } catch (NoSuchDocument e) {
                continue;
            }
        }
    }

    @Override //STUFE 1
    public Document searchForDocument(String documentName) throws CANException {

        Document doc = null;

        for(int i = 0; i < (networkXSize * getNetworkYSize); i++) {
            Position p = hashDocument(documentName,i); //calculate position

            ClientInterface c = searchForResponsibleClient(p); //search for the client responsible for the position

            try { //search for the responsible client
                doc = c.getDocument(documentName);
                return doc;
            } catch (NoSuchDocument ne) {
                continue;
            }
        }
        return doc; //null
    }

    public Position hashDocument(String doc, int i) {
        double m = (networkXSize * getNetworkYSize);

        double summe = 0;

        for(char c : doc.toCharArray()) {
            summe += Character.valueOf(c) - Character.valueOf('a');
        }

        double rFunc = i * (( 2 * ( summe % (m-2))) +1);

        double hashFunc = ((summe % m) + rFunc) % m;

        double hashX = hashFunc % networkXSize;

        double hashY = hashFunc / networkXSize;

        return new Position(hashX,hashY);
    }

    private double min(double a, double b) {
        return (a < b) ? a : b;
    }

    private double max(double a, double b) {
        return (a > b) ? a : b;
    }

    private Pair<ClientInterface,Double> lexiCompare(Pair<ClientInterface,Double> a, Pair<ClientInterface,Double> b) {

        if(a.first.getUniqueID().compareTo(b.first.getUniqueID()) > 0) return a;
        else return b;
    }
}