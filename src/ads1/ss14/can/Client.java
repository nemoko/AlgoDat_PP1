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
    private int maxNumberOfDocuments;
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
        if(library.containsKey(documentName))
            return library.get(documentName);

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
        position = new Position((lowerX-upperX)/2,(lowerY-upperY)/2);
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
	
	@Override
	public ClientInterface searchForResponsibleClient(Position p) {
        double x = p.getX();
        double y = p.getY();

        if((getArea().getLowerX() <= x && x < getArea().getUpperX())
                                    &&
           (getArea().getLowerY() <= y && y < getArea().getUpperY()))
        {
            return this;
        }
        else //ask a neighbour
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
		//TODO Implement me!
	}

	@Override
	public void addDocumentToNetwork(Document d) throws CANException {
		//TODO Implement me!
	}

	@Override
	public void removeDocumentFromNetwork(String documentName) {
		//TODO Implement me!
	}

	@Override
	public Document searchForDocument(String documentName) throws CANException {

        Document doc = null;

        for(int i = 0; i < m(); i++) {
            Position p = new Position(hashX(documentName,i),hashY(documentName,i));

            ClientInterface c = searchForResponsibleClient(p);

            try {
                doc = c.getDocument(documentName);
                return doc;
            } catch (NoSuchDocument ne) {

                //ask neighbours?
                for(ClientInterface ci : neighbourList) {
                    ci.searchForResponsibleClient(p);
                }


                //if no neighbours are responsible for p
                //c asks neighbour c with the shortest distance between neighbour and p

                continue; //i += 1:

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
