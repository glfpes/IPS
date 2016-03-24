<?php
  if(isset($_POST['submit'])){
    print_r($_FILES);
    move_uploaded_file($_FILES['file']['tmp_name'],'C:\Users\glfpes\Documents\SVN\VLC\vlc\images\uploads\jason.jpg');
	system('cd C:\Users\glfpes\Documents\SVN\VLC\vlc\Debug\ && .\vlc.exe');
    system('cd C:\Users\glfpes\Documents\SVN\VLC\AoA\ && C:\Python27\python.exe .\run.py');
	system('cp C:\Users\glfpes\Documents\SVN\VLC\AoA\log.txt C:\develop\xampp\htdocs\vlc\result');
  }
  echo "<hr/>";
  $fp = fopen('C:\Users\glfpes\Documents\SVN\VLC\vlc\Debug\pairs.txt','r'); 
  if($fp){
    for($i=1;! feof($fp);$i++){
      echo fgets($fp). "<br />";
    } 
  }
  echo "<hr/>";
  $fp = fopen('C:\develop\xampp\htdocs\vlc\result','r'); 
  if($fp){
    for($i=1;! feof($fp);$i++){
      echo fgets($fp). "<br />";
    } 
  }
  echo "<hr/>";
?>


<!DOCTYPE html>
<html>
<head>
</head>
<body>
  <form method="post" action="vlc.php" enctype="multipart/form-data">
    <input type="file" name="file" id="file"/>
    <input type="submit" name="submit"/>
  </form>
  <img src="sample.jpg"/>
</body>
</html>