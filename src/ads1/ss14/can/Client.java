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
         if(library.size() < maxNumberOfDocuments) {
            if(library.containsKey(documentName))
                return library.get(documentName);
        }

        throw new NoSuchDocument();
	}

	@Override
	public void storeDocument(Document d, Position p) throws NoAdditionalStorageAvailable, CANException {
        //TODO position provided only for checking?
		if(library.size() != maxNumberOfDocuments) {
            if(position.equals(p)) {
                library.put(d.getName(),d);
            } else throw new CANException("wrong location");
        } else throw new NoAdditionalStorageAvailable();
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
        position = new Position((lowerX-upperX)/2,(lowerY-upperY)/2); //TODO WHY lower-upper
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
        double x = p.getX();
        double y = p.getY();

        if((getArea().getLowerX() <= x && x < getArea().getUpperX())
                                    &&
           (getArea().getLowerY() <= y && y < getArea().getUpperY()))
        {
            return this;
        }
        else //find closest neighbour
        {
            double closestDist = 0.0;
            Pair<ClientInterface,Double> closestNeighbour = new Pair<ClientInterface, Double>(this,closestDist);

            for(int i = 0; i < neighbourList.size(); i++)
            {
                ClientInterface cv = neighbourList.get(i);

                double rX = max(min(cv.getArea().getUpperX(),x),cv.getArea().getLowerX());
                double rY = max(min(cv.getArea().getUpperY(),y),cv.getArea().getLowerY());

                double neighbourDistance = sqrt( (rX - x)*(rX - x) + (rY - y)*(rY - y) ); //Q

                Pair<ClientInterface,Double> neighbour = new Pair<ClientInterface, Double>(cv,neighbourDistance);

                if(i == 0) //init the shortest path
                    closestNeighbour.second = neighbour.second;
                else
                {
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
            }
            //retrieve the client with the shortest distance
            return closestNeighbour.first;
        }
	}

	@Override
	public ClientInterface joinNetwork(ClientInterface entryPoint, Position p) throws CANException {
		//TODO implemented?
        //identify which zone can be split, or does the entry point get split automatically?

        if(entryPoint == null)
            return this;

        Area entry = entryPoint.getArea();
        Pair<Area, Area> pair;

        /* SPLITTING AREA */
        if(entry.getUpperY()-entry.getLowerY() > entry.getUpperX() - entry.getLowerX())
            pair = entry.splitVertically();

        else pair = entry.splitHorizontally();

        this.setArea(pair.first);
        entryPoint.setArea(pair.second);

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
               if (getArea().getUpperY() == neighbor.getArea().getLowerY()) {

                   //neighbor belongs only to the joining client
                   if(neighbor.getArea().getLowerX() >= joiningClient.getArea().getLowerX()) {
                       joiningClient.addNeighbour(neighbor);
                       neighbor.addNeighbour(joiningClient);
                       neighbor.removeNeighbour(this.getUniqueID());
                       this.removeNeighbour(neighbor.getUniqueID());
                       continue;
                   }

                   //both share the neighbor
                   if(neighbor.getArea().getLowerX() < joiningClient.getArea().getLowerX()) {
                       joiningClient.addNeighbour(neighbor);
                       neighbor.addNeighbour(joiningClient);
                   }
               }

               //bottom neighbors
               if(getArea().getLowerY() == neighbor.getArea().getUpperY()) {

                    //neighbor belongs only to the joining client
                    if(neighbor.getArea().getLowerX() >= joiningClient.getArea().getLowerX()) {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                        neighbor.removeNeighbour(this.getUniqueID());
                        this.removeNeighbour(neighbor.getUniqueID());
                        continue;
                    }

                   //both share the neighbor
                   if(neighbor.getArea().getLowerX() < joiningClient.getArea().getLowerX()) {
                       joiningClient.addNeighbour(neighbor);
                       neighbor.addNeighbour(joiningClient);
                   }
               }

               //right side
               if(neighbor.getArea().getLowerX() == joiningClient.getArea().getUpperX()) {
                   joiningClient.addNeighbour(neighbor);
                   this.removeNeighbour(neighbor.getUniqueID());
               }

           }
           return;
        }

        /*********************
        *  {joining client}  *
        *                    *
        *--------------------*
        *                    *
        *                    *
        **********************/
        //TODO Implement me!
        if(this.getArea().getUpperY() == joiningClient.getArea().getLowerY()) {

            for(ClientInterface neighbor : neighbourList)
            {
                //left side
                if(joiningClient.getArea().getLowerX() == neighbor.getArea().getUpperX()) {

                    //share the neighbor
                    if(joiningClient.getArea().getLowerY() > neighbor.getArea().getLowerY()) {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                        continue;
                    }

                    //neighbor belongs to joining client only
                    if(joiningClient.getArea().getLowerY() <= neighbor.getArea().getLowerY()) {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                        neighbor.removeNeighbour(this.getUniqueID());
                        this.removeNeighbour(neighbor.getUniqueID());
                    }
                }

                //right side
                if(joiningClient.getArea().getUpperX() == neighbor.getArea().getLowerX()) {

                    //share the neighbor
                    if(joiningClient.getArea().getLowerY() > neighbor.getArea().getLowerY()) {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                        continue;
                    }

                    //neighbor belongs to the joining client only
                    if(joiningClient.getArea().getLowerX() <= neighbor.getArea().getLowerY()) {
                        joiningClient.addNeighbour(neighbor);
                        neighbor.addNeighbour(joiningClient);
                        neighbor.removeNeighbour(this.getUniqueID());
                        this.removeNeighbour(neighbor.getUniqueID());
                    }
                }

                //top side
                if(joiningClient.getArea().getUpperY() == neighbor.getArea().getLowerY()) {
                    //belongs to joining client only
                    joiningClient.addNeighbour(neighbor);
                    neighbor.addNeighbour(joiningClient);
                    neighbor.removeNeighbour(this.getUniqueID());
                    this.removeNeighbour(neighbor.getUniqueID());
                }

                //bottom stays, nothing to do
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
        return (hashF(summe(Docname), i)) / networkXSize; //not networkYsize?
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
