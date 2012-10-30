package Message;


public class Reply extends Message {

    public int v; // view id
    public int s; // seq no
    public String x; // answer


    // returns 1 on error, 0 on success
    public int fromString(String[] data) {

        // check if there are enough elements to decode...
        if (data.length != 4) {

            return 1;
        }

        // ...if first is int
        try {
          v = Integer.parseInt(data[1]);

        } catch (NumberFormatException e) {

          return 1;
        }

        // ...if second is int
        try {
          s = Integer.parseInt(data[2]);

        } catch (NumberFormatException e) {

          return 1;
        }

        x = data[3];

        return 0;
    }

    public String toString() {


        return "REPLY;" + v + ";" + s + ";" + x;
    }
}