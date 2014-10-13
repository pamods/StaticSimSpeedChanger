
package staticsimspeedchanger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import org.json.JSONObject;

public class StaticSimSpeedChanger
{
    static int speedup_factor = 3;//1.0/(0.8^speedup_factor)
    static String program_version="0.1dev";
    static String dir = "C:\\Games\\Uber Entertainment\\Planetary Annihilation Launcher\\Planetary Annihilation\\stable";
        
    static double mult = 1.0d;
    static double div;
        
    public static void help()
    {
        System.out.println("StaticSimSpeedChanger v"+program_version);
        System.out.println("");
        System.out.println("Usage: StaticSimSpeedChanger [ops] [path_to_pa_dir] [speedup_factor]");
        System.out.println("");
        System.out.println("  [ops]");
        System.out.println("    --help : This help");
        System.out.println("");
        System.out.println("  path_to_pa_dir : The path to the PA directory, containing the PA executable");
        System.out.println("                   and the media directory");
        System.out.println("");
        System.out.println("  speedup_factor : The number of times you have to call /api/sim_slower to");
        System.out.println("                   have a normal speed game, default 3");
        System.out.println("");
        System.out.println("The formula to determine sim speed is 10*(0.8)^speedup_factor. For example the");
        System.out.println("default of speedup_factor = 3 yields 10*(0.8)^3 = 5.12 FPS");
        System.out.println("");
    }
    
    public static boolean parse_ops(String[]args)
    {
        //no args
        if(args.length==0)
        {
            File test = new File(dir);
            if(test.exists() && test.isDirectory())
            {
                System.out.println("Found PA on the default path. Creating default mod.");
                return true;
            }
            else
            {
                System.out.println("Could not find PA on default path, and a directory was not supplied.\n");
                help();
                System.out.println("\nCould not find PA on default path, and a directory was not supplied.\n");
                return false;
            }
        }
        //help
        for(int i=0;i<args.length;++i)
        {
            if(args[i].startsWith("--h") || args[i].startsWith("-h"))
            {
                help();
                return false;
            }
        }
        int i=0;
        while(i<args.length && args[i].startsWith("-"))
        {
            ++i;
        }
        //new path
        if(i<args.length)
        {
            dir=args[i];
            System.out.println("Using supplied path to PA directory: '"+dir+"'");
            ++i;
        }
        //new speedup factor
        if(i<args.length)
        {
            try
            {
                speedup_factor = Integer.parseInt(args[i]);
            }
            catch(Exception e)
            {
                System.out.println("Error: speedup_factor supplied is not a valid integer.");
                return false;
            }
            if(speedup_factor<0)
            {
                System.out.println("Error: speedup_factor supplied is negative, which is not valid.");
                return false;
            }
            if(speedup_factor==0)
            {
                System.out.println("Error: speedup_factor supplied is 0, meaning no change to the vanilla game is required.");
                return false;
            }
            System.out.println("Using supplied speedup_factor: "+speedup_factor);
            ++i;
        }
        return true;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException
    {
        String media = dir+"\\media";
        File unit_dir = new File(dir+"\\media\\pa");
        ArrayList<File> recursive_listing = new ArrayList<>();
        ArrayList<File> json = new ArrayList<>();
        String[] fields = {"navigation.move_speed", "navigation.acceleration", "navigation.brake", "navigation.turn_speed", "pitch_rate", "yaw_rate", "production.metal", "production.energy", "rate_of_fire", "initial_velocity", "max_velocity", "lifetime", "construction_demand.energy", "construction_demand.metal", "ammo_demand", "atrophy_cool_down", "atrophy_rate", "factory_cooldown_time", "navigation.hover_time", "navigation.wobble_speed", "physics.air_friction", "physics.gravity_scalar"};
        boolean[] increase = {                true,                      true,               true,                    true,         true,       true,               true,                true,           true,               true,           true,      false,                         true,                        true,          true,               false,           true,                   false,                   false,                      true,                  false,                     true};
        int[] counts = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        DecimalFormat df = new DecimalFormat();
        
        int total_files_altered=0;
        int total_fields_altered = 0;
        
        if(!parse_ops(args))
        {
            return;
        }
        
        BufferedReader version_reader = new BufferedReader(new FileReader(new File(dir+"\\version.txt")));
        String version = version_reader.readLine();
        version_reader.close();
        System.out.println("version: "+program_version+"-"+version+"\n");
        
        for(int g=0;g<speedup_factor;++g)
        {
            mult*=0.8d;    
        }
        div=mult;
        mult=1.0d/mult;
        
        System.out.println("Assuming sim speed will be reduced to "+(10.0d/mult)+" FPS");
        System.out.println("Everything needs to be made "+mult+"x quicker to compensate\n");
        
        //find json files
        recursive_listing.add(unit_dir);
        for(int i=0;i<recursive_listing.size();++i)
        {
            File curr = recursive_listing.get(i);
            if(curr.isDirectory())
            {
                File[] list = curr.listFiles();
                for(File f:list)
                {
                    recursive_listing.add(f);
                }
            }
            else if(curr.isFile())
            {
                if(curr.getName().endsWith(".json"))
                {
                    json.add(curr);
                }
            }
        }
        
        //alter fields as necessary
        for(File f:json)
        {
            boolean altered=false;
            StringBuilder str=new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while( (line=br.readLine())!=null )
            {
                str.append(line);
            }
            String json_str = str.toString();
            String curr_field;
            
            JSONObject jso = new JSONObject((String)json_str);
            {
                for(int g=0;g<fields.length;++g)
                {
                    if(!fields[g].contains("."))
                    {
                        curr_field = fields[g];
                    }
                    else
                    {
                        curr_field = fields[g].substring(0, fields[g].indexOf("."));
                    }
                    
                    {
                        if(!jso.isNull(curr_field))
                        {
                            if(jso.get(curr_field) instanceof Integer)
                            {
                                jso.put(curr_field, ((Integer)jso.get(curr_field))*(increase[g]?mult:div));
                                altered=true;
                                ++counts[g];
                            }
                            else if(jso.get(curr_field) instanceof Long)
                            {
                                jso.put(curr_field, ((Long)jso.get(curr_field))*(increase[g]?mult:div));
                                altered=true;
                                ++counts[g];
                            }
                            else if(jso.get(curr_field) instanceof Double)
                            {
                                jso.put(curr_field, ((Double)jso.get(curr_field))*(increase[g]?mult:div));
                                altered=true;
                                ++counts[g];
                            }
                            else if(jso.get(curr_field) instanceof JSONObject)
                            {
                                JSONObject nested = (JSONObject) jso.get(curr_field);
                                if(fields[g].contains("."))
                                {
                                    curr_field = fields[g].substring(fields[g].indexOf(".")+1);
                                    if(!nested.isNull(curr_field))
                                    {
                                        if(nested.get(curr_field) instanceof Integer)
                                        {
                                            nested.put(curr_field, ((Integer)nested.get(curr_field))*(increase[g]?mult:div));
                                            altered=true;
                                            ++counts[g];
                                        }
                                        else if(nested.get(curr_field) instanceof Long)
                                        {
                                            nested.put(curr_field, ((Long)nested.get(curr_field))*(increase[g]?mult:div));
                                            altered=true;
                                            ++counts[g];
                                        }
                                        else if(nested.get(curr_field) instanceof Double)
                                        {
                                            nested.put(curr_field, ((Double)nested.get(curr_field))*(increase[g]?mult:div));
                                            altered=true;
                                            ++counts[g];
                                        }
                                    }
                                }
                            }
                            else if(jso.get(curr_field) instanceof JSONObject)
                            {
                                //arrays TODO, if even necessary
                            }
                        }
                    }
                }
            }
            
            if(altered)
            {
                ++total_files_altered;
                String relative = f.getPath().substring(media.length());
                File out_file = new File("StaticSimSpeedChanger_Output/"+version+"/com.pa.trialq.tStaticSimSpeedChanger."+df.format(10.0d/mult)+relative);
                out_file.getParentFile().mkdirs();
                PrintWriter pw = new PrintWriter(out_file);
                pw.write(jso.toString());
                pw.close();
            }
        }
        
        
        Date d = new Date();
        //create modinfo.json
        StringBuilder modinfo = new StringBuilder();
        modinfo.append("{\"context\":\"server\",\"identifier\":\"com.pa.trialq.tStaticSimSpeedChanger.").append(df.format(10.0d/mult)).append("\",\"display_name\":\"Static Sim Speed, ");
        modinfo.append(df.format(10.0d/mult));
        modinfo.append(" FPS\",\"description\":\"Changes unit stats, so that the game plays the same at a lower sim speed. For use with local servers where sim speed can be changed (see forum).\",\"author\":\"trialq\",\"version\":\"");
        modinfo.append(program_version).append("-").append(version).append("\",\"build\":\"").append(version).append("\",\"date\":\"");
        modinfo.append(d.getYear()+1900).append("/").append(String.format("%02d", d.getMonth()+1)).append("/").append(String.format("%02d", d.getDate()));
        modinfo.append("\",\"signature\":\"notyetimplemented\",\"category\":[\"offline-server-mod\"],\"priority\":100,\"enabled\":true}");
        
        File out_file = new File("StaticSimSpeedChanger_Output/"+version+"/com.pa.trialq.tStaticSimSpeedChanger."+df.format(10.0d/mult)+"/modinfo.json");
        out_file.getParentFile().mkdirs();
        PrintWriter pw = new PrintWriter(out_file);
        pw.write(modinfo.toString());
        pw.close();

        
        
        for(int g=0;g<fields.length;++g)
        {
            total_fields_altered+=counts[g];
            System.out.println("\""+fields[g]+"\" fields "+(increase[g]?"increased":"reduced")+": "+counts[g]);
        }
        System.out.println("\n"+total_fields_altered+" fields altered in "+total_files_altered+" files.");
    }
}
