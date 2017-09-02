<?php                                                                                                                               
include_once '../xml_parser.php';                                                                                                   
include_once '../xml_class.php';                                                                                                    
include_once '../directory.php';                                                                                                    
include_once '../lib.php';                                                                                                          
include_once '../lang_bridge.php';                                                                                                  
include_once 'mod_radius.php';                                                                                                      
global $XML_HOME, $CONF_HOME;                                                                                                       
$mode = $_REQUEST["mode"];                                                                                                          
$user_id = $_REQUEST["id"];                                                                                                         
$user_pwd = base64_decode($_REQUEST["pwd"]);                                                                                        
$name = $_REQUEST["name"];                                                                                                          

if ($mode == 2) // remote                                                                                                           
{
    $rad_conf = loadXML("$XML_HOME/network_radius.xml","radius");                                                                   
    $rad_conf = $rad_conf[0];                                                                                                       
    $isON_rad = xget($rad_conf,"radius@chk_use");                                                                                   
    if ( $isON_rad == "on")                                                                                                         
    {                                                                                                                               
        $rad_ip = xget($rad_conf,"ip");                                                                                             
        $rad_pwd = xget($rad_conf,"password");                                                                                      
        $rad_port = xget($rad_conf,"auth_port");       
        
        if ( $rad_ip && $rad_port && $rad_pwd ) 
        {
           $radius = new Radius($rad_ip, $rad_pwd);    
           
           if ( $rad_port && $rad_port != "1812" )        
              $radius->SetAuthenticationPort($rad_port);  
              
           if ($radius->AccessRequest($user_id, $user_pwd))        
           {
              sleep(10);
              system("echo 1 > /tmp/$name"); 
              return;
           }
        }
    }
    sleep(10);    
    system("echo 2 > /tmp/$name");
}
else //  local    
{
    $usrdb = loadXML("$XML_HOME/object_user_list.xml","user");         
    $usrcnt = count($usrdb);   
    for($i=0; $i < $usrcnt; $i++)   
    {
        $mode = xget($usrdb[$i], "setting@mode");    
        if ( $mode == "allow") 
        {
            $id = xget($usrdb[$i], "id"); 
            $pw = xget($usrdb[$i], "password");  
            if ( $id == $user_id && $pw == $user_pwd )   
            {
            	system("echo 1 > /tmp/$name");
            	return;    
            }
        }
    }
    system("echo 2 > /tmp/$name");  
    
    return;  
}
      
         
