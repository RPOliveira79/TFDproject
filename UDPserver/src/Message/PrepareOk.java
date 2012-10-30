package Message;


public class PrepareOk extends Message {

     public int v; // view number
     public int n; // op number
     public int i; // replica id


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
             n = Integer.parseInt(data[2]);

          } catch (NumberFormatException e) {

             return 1;
          }

          // ...if third is int
          try {
             i = Integer.parseInt(data[3]);

           } catch (NumberFormatException e) {

             return 1;
            }

          return 0;
     }

     public String toString() {

          return "PREPAREOK;" + v + ";" + n + ";" + i;
     }
}