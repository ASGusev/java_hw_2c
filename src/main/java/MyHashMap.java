public class MyHashMap {

    // хеш-таблица, использующая список
 
    // ключами и значениями выступают строки

    // стандартный способ получить хеш объекта -- вызвать у него метод hashCode()

    // сейчас все методы бросают исключение
    // это сделано, чтобы код компилировался, в конечном коде такого исключения быть не должно
    
    private final static int INITIAL_CAPACITY = 2017;

    private int size = 0;
    private int capacity = INITIAL_CAPACITY;
    private List[] store = new List[capacity];
    
    MyHashMap() {
		for (int i = 0; i < capacity; i++) {
			store[i] = new List();
		}
	}
	
	private void migrate(int newCap) {
		List[] newStorage = new List[newCap];
		for (int i = 0; i < newCap; i++) {
			newStorage[i] = new List();
		}
		for (int i = 0; i < capacity; i++) {
			while (!store[i].empty()) {
				Entry curEntry = store[i].pop();
				newStorage[curEntry.hash % newCap].add(curEntry);
			}
		}
		store = newStorage;
		capacity = newCap;
	}

    public int size() {
        // кол-во ключей в таблице
        return size;
    }

    public boolean contains(String key) {
        // true, если такой ключ содержится в таблице
        int cellNumber = key.hashCode() % capacity;
        return store[cellNumber].exists(key);
    }

    public String get(String key) {
        // возвращает значение, хранимое по ключу key
        // если такого нет, возвращает null
        int cellNumber = key.hashCode() % capacity;
        return store[cellNumber].get(key);
    }

    public String put(String key, String value) {
        // положить по ключу key значение value
        // и вернуть ранее хранимое, либо null
        int cellNumber = key.hashCode() % capacity;
		String ret = store[cellNumber].get(key);
        if (ret == null) {
			store[cellNumber].add(new Entry(key, value));
			size++;
		} else {
			store[cellNumber].change(key, value);
		}
		if (size == capacity) {
			migrate(capacity * 2);
		}
		return ret;
    }

    public String remove(String key) {
        // забыть про пару key-value для переданного value
        // и вернуть забытое value, либо null, если такой пары не было
        //throw new UnsupportedOperationException();
        int cellNumber = key.hashCode() % capacity;
        String ans = store[cellNumber].remove(key);
        if (ans != null) {
			size--;
		}
		return ans;
    }

    public void clear() {
        // забыть про все пары key-value
        capacity = INITIAL_CAPACITY;
        store = new List[capacity];
		for (int i = 0; i < capacity; i++) {
			store[i] = new List();
		}
        size = 0;
    }
	
	private class Entry {
		int hash;
		String key, val;
		
		Entry(String newKey, String newVal) {
			key = newKey;
			val = newVal;
			hash = key.hashCode();
		}
	}
	
    private class List {
		Node head;
		
		List() {
			head = null;
		}
		
		public boolean exists(String key) {
			Node pos = head;
			while (pos != null && !pos.val.key.equals(key)) {
				pos = pos.next;
			}
			return pos != null;
		}
        
        public void add(Entry newEntry) {
			Node newNode = new Node(newEntry);
			newNode.next = head;
			head = newNode;
		}
		
		public void change(String key, String val) {
			Node pos = head;
			while (!pos.val.key.equals(key)) {
				pos = pos.next;
			}
			pos.val.val = val;
		}
        
        public String get(String key) {
			Node pos = head;
			String ret = null;
			while (pos != null && !pos.val.key.equals(key)) {
				pos = pos.next;
			}
			if (pos != null) {
				ret = pos.val.val;
			}
			return ret;
		}
		
		public String remove(String key) {
			String ans = null;
			if (head != null) {
				if (head.val.key.equals(key)) {
					ans = head.val.val;
					head = head.next;
				} else {
					Node pos = head;
					while (pos.next != null && !pos.next.val.key.equals(key)) {
						pos = pos.next;
					}
					if (pos.next != null) {
						ans = pos.next.val.val;
						pos.next = pos.next.next;
					}
				}
			}
			return ans;
		}
		
		public Entry pop() {
			Entry res = head.val;
			head = head.next;
			return res;
		}
		
		public boolean empty() {
			return head == null;
		}
		
		public void clear() {
			head = null;
		}
    
        private class Node {
            public Entry val;
            public Node next;
            
            Node(Entry newVal) {
				val = newVal;
				next = null;
			}
        }
	}
}
