package ads1.ss14.can;

import ads1.ss14.can.exceptions.CANException;
import ads1.ss14.can.exceptions.NoAdditionalStorageAvailable;
import ads1.ss14.can.exceptions.NoSuchDocument;

import java.util.ArrayList;
import java.util.HashMap;

public class Client implements ClientInterface, ClientCommandInterface{

    private String uniqueID;
    private int networkXSize;
    private int getNetworkYSize;
    private int maxNumberOfDocuments;
    private HashMap<String,Document> library;
    private Position position;
    private Area area;
    private ArrayList<Client> neighbourList;

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
		//TODO Implement me!
		return null;
	}
	
	@Override
	public void addNeighbour(ClientInterface newNeighbour){
		//TODO Implement me!
	}
	
	@Override
	public void removeNeighbour(String clientID) {
		//TODO Implement me!
	}
	
	@Override
	public ClientInterface searchForResponsibleClient(Position p) {
        //TODO Implement me!
        // calculate Position using
        // Hx(D.name,i), Hy(D.name,i) => p(x,y)

        /*
        Then search for the client responsible for p
        C.area=[(x1,x2);(y1,y2)]

        C.area.x1 < p.x < C.area.x2
                    &&
        C.area.y1 < p.y < C.area.y2

        //if the search lands in C, end

        //else
        ask a neighbour with the shortest euklidian distance Q(C.area,p) between the area of the N and p-searchP

        Q(N.area,p) =
        r.x = max(min(C.area.x2,p.x),C.area.x1)
        r.y = max(min(C.area.y2,p.y),C.area.y1)

        Q(C.area,p) = sqr( (r.x - p.x)^2 + (r.y - p.y)^2 )

            //if more clients with the same distance to p exist, choose the lowest lexicological ID(String.compareTo)

        // if shit happens, i += 1;


        */

		return null;
	}

	@Override
	public ClientInterface joinNetwork(ClientInterface entryPoint, Position p) throws CANException {
		//TODO implemented?

        Area entry = entryPoint.getArea();
        Pair<Area, Area> pair;

        /* SPLITTING AREA */
        if(entry.getUpperY()-entry.getLowerY() > entry.getUpperX() - entry.getLowerX())
            pair = entry.splitVertically();

        else pair = entry.splitHorizontally();

        this.setArea(pair.first);
        entryPoint.setArea(pair.second);
        /* END */

		return entryPoint; //return the input???? i think so
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
		//TODO Implement me!
		return null;
	}

    /*********************************************
     *                                            *
     *               HASHFUNCTION                 *
     *                                            *
     **********************************************/
    private int m() {
        return networkXSize * getNetworkYSize;
    }

    private int rFunc(int summe, int i) {
        return i * (( 2 * ( summe % (m()-2))) +1);
    }

    private int summe(String name) {
        int summe = 0;

        for(int x = 0; x < name.length(); x++) {
            char c = name.charAt(x);
            summe = Character.valueOf(c);
        }

        return summe;
    }

    private int hashF(int summe, int i) {
        return ((summe % m()) + rFunc(summe,i)) % m();
    }

    private int hashX(String Docname, int i) {
        return hashF(summe(Docname), i) % networkXSize;
    }

    private int hashY(String Docname, int i) {
        return (hashF(summe(Docname), i)) / networkXSize;
    }
    /***********************************************/
}
