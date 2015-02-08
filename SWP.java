import java.util.*;
import java.awt.event.*;
/*===============================================================*
 *  File: SWP.java                                               *
 *                                                               *
 *  This class implements the sliding window protocol            *
 *  Used by VMach class					         *
 *  Uses the following classes: SWE, Packet, PFrame, PEvent,     *
 *                                                               *
 *  Author: Professor SUN Chengzheng                             *
 *          School of Computer Engineering                       *
 *          Nanyang Technological University                     *
 *          Singapore 639798                                     *
 *===============================================================*/

public class SWP {

/*========================================================================*
 the following are provided, do not change them!!
 *========================================================================*/
   //the following are protocol constants.
   public static final int MAX_SEQ = 7; 
   public static final int NR_BUFS = (MAX_SEQ + 1)/2;

   public Timer seqTimer[] = new Timer[NR_BUFS];
   public Timer ackTimer = null;

   public boolean no_nak = true; //Shows that no NAK has been sent yet for frame-expected



   // the following are protocol variables
   private int oldest_frame;
   private PEvent event = new PEvent();  
   private Packet out_buf[] = new Packet[NR_BUFS];

   
   //the following are used for simulation purpose only
   private SWE swe = null;
   private String sid = null;  

   //Constructor
   public SWP(SWE sw, String s){
      swe = sw;
      sid = s;
   }

   //the following methods are all protocol related
   private void init(){
      for (int i = 0; i < NR_BUFS; i++){
	   out_buf[i] = new Packet();
         
      }
    
  
   }

   private void wait_for_event(PEvent e){
      swe.wait_for_event(e); //may be blocked
      oldest_frame = e.seq;  //set timeout frame seq
   }

   private void enable_network_layer(int nr_of_bufs) {
   //network layer is permitted to send if credit is available
	swe.grant_credit(nr_of_bufs);
   }

   private void from_network_layer(Packet p) {
      swe.from_network_layer(p);
   }

   private void to_network_layer(Packet packet) {
	swe.to_network_layer(packet);
   }

   private void to_physical_layer(PFrame fm)  {
      System.out.println("SWP: Sending frame: seq = " + fm.seq + 
			    " ack = " + fm.ack + " kind = " + 
			    PFrame.KIND[fm.kind] + " info = " + fm.info.data );
      System.out.flush();
      swe.to_physical_layer(fm);
   }

   private void from_physical_layer(PFrame fm) {
      PFrame fm1 = swe.from_physical_layer(); 
	fm.kind = fm1.kind;
	fm.seq = fm1.seq; 
	fm.ack = fm1.ack;
	fm.info = fm1.info;
   }


/*===========================================================================*
 	implement your Protocol Variables and Methods below: 
 *==========================================================================*/

   private boolean between(int a, int b, int c)
   {
       //Is b within a and c
       boolean x  = ((a<=b)&&(b<c)) || ((c<a)&&(a<=b)) || ((b<c)&&(c<a));
       System.out.println("between "+ a + " " + b + " " + c);
       return x;
   }
   private PFrame send_frame(int frame_type, int frame_nr, int next_frame_expected, Packet buffer[])
   {
                   PFrame fml =  new PFrame();
                   
                   fml.kind = frame_type;
                   
                   if(frame_type == PFrame.DATA)//Data
                   {
                       
                        fml.info = buffer[frame_nr%NR_BUFS];
                       
                   } 
                   
                   fml.seq = frame_nr; 
                   fml.ack = ((next_frame_expected+MAX_SEQ)%(MAX_SEQ+1));
                  // fml.ack = next_frame_expected%NR_BUFS;
                   if(frame_type == PFrame.NAK)//NAK
                   {
                       no_nak = false;
                       
                        
                   }
                   to_physical_layer(fml);
                   
                   if(frame_type == PFrame.DATA)
                   {
                      
                       start_timer(frame_nr%NR_BUFS);
                       
                       
                   }
                   stop_ack_timer();
                   return fml;
   }
   private int inc(int incrementNumber)
   {
       int inside = incrementNumber;
       System.out.println("Received"+inside);
       
       inside++;
       System.out.println("Bef"+inside);
       inside = inside%(MAX_SEQ+1);
       System.out.println("Aft"+inside);
       return inside;
   }
  
   
        private int sender_ack_expected;        //sender lower window
        private int sender_next_frame_to_send;  //sender upper window
        
        private int receiver_frame_expected;    //reciever lower window
        private int receiver_too_far;           //receiver upper window
        
        private int i;
        private PFrame fml =  new PFrame();     //scratch variable
        
        private boolean[] arrived = new boolean[NR_BUFS];//keep track whether the seq frame have arrived
        private int nbuffered;
        
        private Packet in_buf[] = new Packet[NR_BUFS];// buffer to keep for in coming packets
      
        
        
        
   public void protocol6() {
        sender_ack_expected = 0;
        sender_next_frame_to_send = 0;
        receiver_frame_expected = 0;
        receiver_too_far = NR_BUFS;
        
        init();
        
      
      //  nbuffered = 0;
        
        enable_network_layer(NR_BUFS);// Gives 4 credit to the network layer
        
        for(i = 0; i<NR_BUFS;i++)//initalise the arrived buffer to all false, not arrived
        {
            arrived[i] = false;
        }
            while(true) 
            {	
             wait_for_event(event);
               switch(event.type) {
                  case (PEvent.NETWORK_LAYER_READY):
                      
                       System.out.println("Network Layer Ready");
                       System.out.println("Bef Sender "+sender_ack_expected+" "+sender_next_frame_to_send);
                       
                       //nbuffered = nbuffered + 1;
                       from_network_layer(out_buf[sender_next_frame_to_send%NR_BUFS]);// Get packets from the network layer, 
                        
                       send_frame(PFrame.DATA,sender_next_frame_to_send,receiver_frame_expected,out_buf);
                       sender_next_frame_to_send = inc(sender_next_frame_to_send);
                      // receiver_frame_expected = inc(receiver_frame_expected);
                       
                       System.out.println("Aft Sender "+sender_ack_expected+" "+sender_next_frame_to_send);
                       
                       break; 
                  case (PEvent.FRAME_ARRIVAL ):
                       
                       from_physical_layer(fml);
                       System.out.println("Frame Arrival " +  "INFO: " + fml.info.data + "KIND: " + fml.kind + "SEQ: " + fml.seq);
                       
                       if(fml.kind == PFrame.DATA)
                       {
                           if((fml.seq != receiver_frame_expected)&& no_nak)
                           {
                               System.out.println("Not perfect frame!");
                               send_frame(PFrame.NAK,0,receiver_frame_expected,out_buf);
                           }
                           else
                           {
                               start_ack_timer();
                           }
                           
                            System.out.println("Bef Reciever Window: "+ receiver_frame_expected + " " + receiver_too_far);
                            if(between(receiver_frame_expected,fml.seq,receiver_too_far)&&(arrived[fml.seq%NR_BUFS]==false))
                            {
                                arrived[fml.seq%NR_BUFS] = true;
                                in_buf[fml.seq%NR_BUFS] = fml.info;

                                while(arrived[receiver_frame_expected%NR_BUFS])
                                {
                                    System.out.println("Perfect Frame and accepted!");
                                    to_network_layer(in_buf[fml.seq%NR_BUFS]);
                                    no_nak = true;
                                    arrived[receiver_frame_expected%NR_BUFS] = false;
                                    //nbuffered = nbuffered-1;
                                    receiver_frame_expected = inc(receiver_frame_expected);
                                    receiver_too_far = inc(receiver_too_far);
                                    
                                    System.out.println("Reciever Window: "+ receiver_frame_expected + " " + receiver_too_far);
                                    start_ack_timer();
                                    
                                }
                            }
                       }
                       if((fml.kind == PFrame.NAK) && between(sender_ack_expected,((fml.ack+1)%(MAX_SEQ+1)),sender_next_frame_to_send))
                       {
                            send_frame(PFrame.DATA,(fml.ack+1)%(MAX_SEQ+1),receiver_frame_expected,out_buf);
                            
                           
                       }
                      
                         System.out.println("Sender "+sender_ack_expected + " " + fml.ack + " " +sender_next_frame_to_send);
                        while(between(sender_ack_expected,fml.ack,sender_next_frame_to_send))
                        {
                                System.out.println("ACK "+sender_ack_expected);
                                //nbuffered = nbuffered-1; 
                              
                                
                                stop_timer(sender_ack_expected%NR_BUFS);
                                stop_ack_timer();
                                System.out.println("Stop Timer" + sender_ack_expected%NR_BUFS);
                                enable_network_layer(1);
                                
                             
                                sender_ack_expected = inc(sender_ack_expected);
                        }
                      
                       break;	   
                  case (PEvent.CKSUM_ERR):
                      System.out.println("CKSUM Error");
                      if(no_nak)
                      {
                          send_frame(PFrame.NAK,0,receiver_frame_expected,out_buf);
                      }
                       break;  
                  case (PEvent.TIMEOUT): 
                      System.out.println("Timeout!");
                       send_frame(PFrame.DATA,oldest_frame,receiver_frame_expected,out_buf);
                       break; 
                  case (PEvent.ACK_TIMEOUT): 
                      System.out.println("Ack Timeout");
                       send_frame(PFrame.ACK,0,receiver_frame_expected,out_buf);
                       stop_ack_timer();
                       break; 
                default: 
                       System.out.println("SWP: undefined event type = " 
                                           + event.type); 
                       System.out.flush();
                
               
             
        }       
            //   System.out.println("Out");
               // enable_network_layer(1);
             
              // System.out.println("nubuffered"+nbuffered);
             /*
               if(nbuffered < NR_BUFS)
               {
                   
              
                   enable_network_layer(NR_BUFS);
               }
               */
               
              
               
               
             
      }      
   }

 /* Note: when start_timer() and stop_timer() are called, 
    the "seq" parameter must be the sequence number, rather 
    than the index of the timer array, 
    of the frame associated with this timer, 
   */
class normalTimer extends TimerTask
{
     
    private final int seq;
    
    normalTimer(int seq)
    {
        this.seq = seq;
    }
    public void run() 
    {
         System.out.println("seq "+seq);
         swe.generate_timeout_event(seq);
         System.out.println("Generated Timeout");
 
          
    }
}
class ackTimerTask extends TimerTask
{
  
   
    public void run() 
    {
       
         swe.generate_acktimeout_event();
         System.out.println("Generated Ack Timeout");
 
          
    }
}
   private void start_timer(int seq) {
    
   
       
    normalTimer nTim = new normalTimer(seq);
    //seqTimer[seq].scheduleAtFixedRate(new TimerTask(){
     try
     {
      seqTimer[seq] = new Timer();
      System.out.println("Timer Started "+ seq);
      seqTimer[seq].schedule(nTim, 50);
     }
     catch(Exception ex)
     {
         System.out.println("error"+ ex);
     }

   }

   private void stop_timer(int seq) {
       try
       {
         System.out.println("Cancel"+seq);
         if(seqTimer[seq] != null)
         {
            seqTimer[seq].cancel();
         }
       }
       catch(Exception ex)
       {
       
       }
   }

   private void start_ack_timer( ) {
      
       
       System.out.println("Timer Started ack");
       ackTimer = new Timer();
       ackTimer.schedule(new ackTimerTask(), 25);
     
   }

   private void stop_ack_timer() {
       try
       {
         if(ackTimer != null)
         {
            System.out.println("Ack Cancel");
            ackTimer.cancel();
         }
        }
       catch(Exception ex)
       {
       
       }
   }

}//End of class

/* Note: In class SWE, the following two public methods are available:
   . generate_acktimeout_event() and
   . generate_timeout_event(seqnr).

   To call these two methods (for implementing timers),
   the "swe" object should be referred as follows:
     swe.generate_acktimeout_event(), or
     swe.generate_timeout_event(seqnr).
*/


