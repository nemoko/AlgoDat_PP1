package ads1.ss14.can;

import ads1.ss14.can.exceptions.CANException;
import ads1.ss14.can.exceptions.NoAdditionalStorageAvailable;
import ads1.ss14.can.exceptions.NoSuchDocument;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Math.sqrt;

public class Client implements ClientInterface, ClientCommandInterface{

    private String uniqueID;
    private int networkXSize;
    private int getNetworkYSize;
    private int maxNumberOfDocuments = -1;
    private HashMap<String,Document> library;
    private Position position;
    private Area area;
    private ArrayList<ClientInterface> neighbourList;

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
                return library.get(documentName);


        throw new NoSuchDocument();
	}

	@Override
	public void storeDocument(Document d, Position p) throws NoAdditionalStorageAvailable, CANException {
        //TODO why am i getting the position here
		if(library.size() < maxNumberOfDocuments)
                library.put(d.getName(),d);

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
		neighbourList.add( (Client) newNeighbour);
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

        int i = 0;
        for(; i < neighbourList.size(); i++) {
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
            }
            else
            {
                closestNeighbour = neighbour;
            }
        }
        //retrieve the client with the shortest distance
        return neighbourList.get(i).searchForResponsibleClient(p);

	}

	@Override
	public ClientInterface joinNetwork(ClientInterface entryPoint, Position p) throws CANException {

		//TODO implement
        //check if p is here and if not
        //search for the Client responsible for p, to split his zone
        //then return


        if(entryPoint == null)
            return this;

        //TODO implement
        //if P entry is here, continue

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

        //TODO implement
        //adapt neighbours
        //move docs

        /* END */
		return entryPoint; //TODO return the input???? i think so
	}

	@Override
	public Iterable<Pair<Document, Position>> removeUnmanagedDocuments() {
		//TODO Implement me!
		return null;
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
                   this.removeNeighbour(neighbor.getUniqueID());
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
        //TODO Implement me!
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
		//TODO Implement me!

	}

	@Override //STUFE 2
	public void removeDocumentFromNetwork(String documentName) {
		//TODO Implement me!
	}

	@Override //STUFE 1
	public Document searchForDocument(String documentName) throws CANException {

        Document doc = null;

        for(int i = 0; i < m(); i++) { //calculate position
            Position p = new Position(hashX(documentName,i),hashY(documentName,i));

            ClientInterface c = searchForResponsibleClient(p); //search for the client responsible for the position

            try { //search for the responsible client
                doc = c.getDocument(documentName);
                return doc;
            } catch (NoSuchDocument ne) { //if fails

                //ask neighbours
                for(ClientInterface ci : neighbourList) {
                    try {
                        doc = ci.getDocument(documentName);
                        return doc;
                    } catch (NoSuchDocument nec) {
                        continue;
                    }
                }

                //if no neighbours are responsible for p
                //ask neighbour with the shortest distance between itself and the calculated position
                c = c.searchForResponsibleClient(p);

                try {
                    doc = c.getDocument(documentName);
                    return doc;
                } catch (NoSuchDocument ned) {
                    continue; //i += 1:
                }
            }
        }
        return doc; //null
	}

    /**********************************************
     *                                            *
     *               HASHFUNCTION                 *
     *                                            *
     **********************************************/
    private double m() {
        return (area.getUpperX() - area.getLowerX()) * (area.getUpperY() - area.getLowerY());
    }

    private double rFunc(int summe, int i) {
        return i * (( 2 * ( summe % (m()-2))) +1);
    }

    private int summe(String name) {
        int summe = 0;

        for(char c : name.toCharArray()) {
            summe += Character.valueOf(c) - Character.valueOf('a');
        }
        return summe;
    }

    private double hashF(int summe, int i) {
        return ((summe % m()) + rFunc(summe,i)) % m();
    }

    private double hashX(String Docname, int i) {
        return hashF(summe(Docname), i) % networkXSize;
    }

    private double hashY(String Docname, int i) {
        return (hashF(summe(Docname), i)) / networkXSize;
    }
    /***********************************************/

    private double min(double a, double b) {
        return (a < b) ? a : b;
    }

    private double max(double a, double b) {
        return (a > b) ? a : b;
    }

    private Pair<ClientInterface,Double> lexiCompare(Pair<ClientInterface,Double> a, Pair<ClientInterface,Double> b) {

        if(a.first.getUniqueID().compareTo(b.first.getUniqueID()) < 0) return a;
        if(a.first.getUniqueID().compareTo(b.first.getUniqueID()) > 0) return b;
        else return a; //TODO should never happen, throw exception?
    }
}
