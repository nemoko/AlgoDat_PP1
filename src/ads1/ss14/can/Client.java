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

	@Override
	public Area getArea() {
		return area;
	}
	
	@Override
	public void setArea(Area newArea) {
		area = newArea;
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
		return null;
	}

	@Override
	public ClientInterface joinNetwork(ClientInterface entryPoint, Position p) throws CANException {
		//TODO Implement me!

        position = p;
		return null;
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
}
