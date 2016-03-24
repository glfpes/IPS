#include<iostream>
#include<string>
#include<fstream>

using namespace std;

int main()
{
	string filename1 = "C:\\develop\\xampp\\htdocs\\vlc\\result_location";
	ifstream infile(filename1.c_str());

	string filename2 = "C:\\develop\\xampp\\htdocs\\vlc\\location_to_android";
	ofstream fout(filename2);
	
	string temp;
	getline(infile, temp);
	cout << temp << endl;
	bool lock = 0;//记录一个数字的记录开始与否，0表示当前游标上一个字符不处在数字范围内，1表示当前游标上一个字符处在数字范围内
	string temp_result = "";
	for (int i = 0; i < temp.size(); i++)
	{
		
		if ((temp[i] == '-' || (temp[i] >= '0' && temp[i] <= '9')) && lock == 0)
			lock = 1;
		else if ((temp[i] == ']' || temp[i] == ' ') && lock == 1)
		{
			lock = 0;
			fout << temp_result<<endl;
			temp_result = "";
		}
			
		if (lock == 1)
		{
			temp_result += temp[i];
		}

	}
	return 0;
}