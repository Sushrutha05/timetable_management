import './App.css';

function ButtonComponent({btn_title}){
  return(
    <button type="button" title={btn_title}>{btn_title}</button>
  );
} 

function TextInput({title, placeholder, type}) {
  return(
    <input type={type} title={title} placeholder={placeholder}/>
  );
}



function App() {
  return (
    <div className="App">
      <div className='project_title'>
        <h1>Timetable Management</h1>
      </div>
      
      <div className="form-container">
        <TextInput title="Email" placeholder="Email" type="text"/>
        <br/>
        <TextInput title="Password" placeholder="Password" type="password"/>
        <br />
        <ButtonComponent btn_title="Login"/>
      </div>
      
    </div>
  );
}

export default App;
