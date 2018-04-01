function main()
{
   // Get the person details and ensure they exist for update
   var userName = url.extension;
   var person = people.getPerson(userName);
   if (person == null)
   {
      status.setCode(status.STATUS_NOT_FOUND, "Person " + userName + " does not exist");
      return;
   }
   
   // assign new values to the person's properties
   if (json.has("firstName"))
   {
      person.properties["firstName"] = json.get("firstName").textValue();
   }
   if (json.has("lastName"))
   {
      person.properties["lastName"] = json.get("lastName").textValue();
   }
   if (json.has("email"))
   {
      person.properties["email"] = json.get("email").textValue();
   }
   if (json.has("title"))
   {
      person.properties["title"] = json.get("title").textValue();
   }
   if (json.has("organisation"))
   {
      person.properties["organization"] = json.get("organisation").textValue();
   }
   if (json.has("jobtitle"))
   {
      person.properties["jobtitle"] = json.get("jobtitle").textValue();
   }
   if (json.has("location"))
   { 
      person.properties["location"] = json.get("location").textValue();
   } 
   if (json.has("telephone"))
   { 
      person.properties["telephone"] = json.get("telephone").textValue();
   } 
   if (json.has("mobile"))
   { 
      person.properties["mobile"] = json.get("mobile").textValue();
   } 
   if (json.has("companyaddress1"))
   { 
      person.properties["companyaddress1"] = json.get("companyaddress1").textValue();
   } 
   if (json.has("companyaddress2"))
   { 
      person.properties["companyaddress2"] = json.get("companyaddress2").textValue();
   } 
   if (json.has("companyaddress3"))
   { 
      person.properties["companyaddress3"] = json.get("companyaddress3").textValue();
   } 
   if (json.has("companypostcode"))
   { 
      person.properties["companypostcode"] = json.get("companypostcode").textValue();
   } 
   if (json.has("companytelephone"))
   { 
      person.properties["companytelephone"] = json.get("companytelephone").textValue();
   } 
   if (json.has("companyfax"))
   { 
      person.properties["companyfax"] = json.get("companyfax").textValue();
   } 
   if (json.has("companyemail"))
   { 
      person.properties["companyemail"] = json.get("companyemail").textValue();
   } 
   if (json.has("skype"))
   { 
      person.properties["skype"] = json.get("skype").textValue();
   } 
   if (json.has("instantmsg"))
   { 
      person.properties["instantmsg"] = json.get("instantmsg").textValue();
   } 
   if (json.has("persondescription"))
   { 
      person.properties["persondescription"] = json.get("persondescription").textValue();
   } 
   // Update the person node with the modified details
   person.save();
   
   // Enable or disable account? - note that only Admin can set this
   if (json.has("disableAccount"))
   {
      var disableAccount = (json.get("disableAccount").booleanVlue() == true);
      if (disableAccount && people.isAccountEnabled(userName))
      {
         people.disableAccount(userName);
      }
      else if (!disableAccount && !people.isAccountEnabled(userName))
      {
         people.enableAccount(userName);
      }
   }
   
   // set quota if supplied - note that only Admin can set this and will be ignored otherwise
   if (json.has("quota"))
   {
      var quota = json.get("quota").asInt();
      people.setQuota(person, quota.toString());
   }
   
   // remove from groups if supplied - note that only Admin can do this
   if (json.has("removeGroups"))
   {
      var groups = json.get("removeGroups");
      for (var index=0; index<groups.size(); index++)
      {
         var groupId = groups.get(index).textValue();
         var group = people.getGroup(groupId);
         if (group != null)
         {
            people.removeAuthority(group, person);
         }
      }
   }
   
   // app to groups if supplied - note that only Admin can do this
   if (json.has("addGroups"))
   {
      var groups = json.get("addGroups");
      for (var index=0; index<groups.size(); index++)
      {
         var groupId = groups.get(index).textValue();
         var group = people.getGroup(groupId);
         if (group != null)
         {
            people.addAuthority(group, person);
         }
      }
   }
   
   // Put the created person into the model
   model.person = person;
}

main();